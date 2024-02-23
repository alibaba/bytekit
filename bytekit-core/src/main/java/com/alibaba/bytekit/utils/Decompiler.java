package com.alibaba.bytekit.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns.LineNumberMapping;

import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import com.alibaba.deps.org.objectweb.asm.util.Printer;
import com.alibaba.deps.org.objectweb.asm.util.Textifier;
import com.alibaba.deps.org.objectweb.asm.util.TraceClassVisitor;
import com.alibaba.deps.org.objectweb.asm.util.TraceMethodVisitor;

/**
 * TODO com.taobao.arthas.core.util.Decompiler
 * 
 * @author hengyunabc
 *
 */
public class Decompiler {

    public static String decompile(byte[] bytecode) throws IOException {
        return decompile(bytecode, false);
    }

    public static String decompile(byte[] bytecode, boolean printLineNumber) throws IOException {
        return decompile(bytecode, null, printLineNumber);
    }

    public static String decompile(byte[] bytecode, String methodName, boolean printLineNumber) throws IOException {
        String result = "";

        File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        File file = new File(tempDirectory, UUID.randomUUID().toString());
        FileUtils.writeByteArrayToFile(file, bytecode);

        result = decompile(file.getAbsolutePath(), methodName, true, printLineNumber);
        return result;
    }

    public static String decompile(String path) throws IOException {
        byte[] byteArray = FileUtils.readFileToByteArray(new File(path));
        return decompile(byteArray);
    }

    public static String toString(MethodNode methodNode) {
        Printer printer = new Textifier();
        TraceMethodVisitor methodPrinter = new TraceMethodVisitor(printer);

        methodNode.accept(methodPrinter);

        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();

        return sw.toString();
    }

    public static String toString(ClassNode classNode) {
        Printer printer = new Textifier();
        StringWriter sw = new StringWriter();
        PrintWriter printWriter = new PrintWriter(sw);

        TraceClassVisitor traceClassVisitor = new TraceClassVisitor(printWriter);

        classNode.accept(traceClassVisitor);

        printer.print(printWriter);
        printer.getText().clear();

        return sw.toString();
    }

    public static String toString(InsnList insnList) {
        Printer printer = new Textifier();
        TraceMethodVisitor mp = new TraceMethodVisitor(printer);
        insnList.accept(mp);

        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        return sw.toString();
    }

    public static String toString(AbstractInsnNode insn) {
        Printer printer = new Textifier();
        TraceMethodVisitor mp = new TraceMethodVisitor(printer);
        insn.accept(mp);

        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        return sw.toString();
    }

    /**
     * @param classFilePath
     * @param methodName
     * @return
     */
    public static String decompile(String classFilePath, String methodName) {
        return decompile(classFilePath, methodName, false);
    }

    /**
     * 
     * @param classFilePath
     * @param methodName
     * @param printLineNumber
     * @return
     */
    public static String decompile(String classFilePath, String methodName, boolean printLineNumber) {
        return decompile(classFilePath, methodName, true, printLineNumber);
    }

    public static String decompile(String classFilePath, String methodName, boolean hideUnicode,
            boolean printLineNumber) {
        final StringBuilder sb = new StringBuilder(8192);
        final NavigableMap<Integer, Integer> lineMapping = new TreeMap<>();
        OutputSinkFactory mySink = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
                return Arrays.asList(SinkClass.STRING, SinkClass.DECOMPILED, SinkClass.DECOMPILED_MULTIVER,
                        SinkClass.EXCEPTION_MESSAGE, SinkClass.LINE_NUMBER_MAPPING);
            }

            @Override
            public <T> Sink<T> getSink(final SinkType sinkType, final SinkClass sinkClass) {
                return sinkable -> {
                    // skip message like: Analysing type demo.MathGame
                    if (sinkType == SinkType.PROGRESS) {
                        return;
                    }
                    if (sinkType == SinkType.LINENUMBER) {
                        LineNumberMapping mapping = (LineNumberMapping) sinkable;
                        NavigableMap<Integer, Integer> classFileMappings = mapping.getClassFileMappings();
                        NavigableMap<Integer, Integer> mappings = mapping.getMappings();
                        if (classFileMappings != null && mappings != null) {
                            for (Entry<Integer, Integer> entry : mappings.entrySet()) {
                                Integer srcLineNumber = classFileMappings.get(entry.getKey());
                                lineMapping.put(entry.getValue(), srcLineNumber);
                            }
                        }
                        return;
                    }
                    sb.append(sinkable);
                };
            }
        };

        Map<String, String> options = new HashMap<>();
        options.put("hideutf", String.valueOf(hideUnicode));
        options.put("trackbytecodeloc", "true");

        /**
         * @see org.benf.cfr.reader.util.MiscConstants.Version.getVersion() Currently,
         *      the cfr version is wrong. so disable show cfr version.
         */
        options.put("showversion", "false");
        if (methodName != null) {
            options.put("methodname", methodName);
        }

        CfrDriver driver = new CfrDriver.Builder().withOptions(options).withOutputSink(mySink).build();
        List<String> toAnalyse = new ArrayList<>();
        toAnalyse.add(classFilePath);
        driver.analyse(toAnalyse);
        String src = sb.toString();
        if (printLineNumber && !lineMapping.isEmpty()) {
            src = addLineNumber(src, lineMapping);
        }
        return src;
    }

    private static String addLineNumber(String src, Map<Integer, Integer> lineMapping) {
        int maxLineNumber = 0;
        for (Integer value : lineMapping.values()) {
            if (value != null && value > maxLineNumber) {
                maxLineNumber = value;
            }
        }
        String formatStr = "/*%2d*/ ";
        String emptyStr = "       ";
        StringBuilder sb = new StringBuilder();
        String[] lines = src.split("\\R");
        ;
        if (maxLineNumber >= 1000) {
            formatStr = "/*%4d*/ ";
            emptyStr = "         ";
        } else if (maxLineNumber >= 100) {
            formatStr = "/*%3d*/ ";
            emptyStr = "        ";
        }
        int index = 0;
        for (String line : lines) {
            Integer srcLineNumber = lineMapping.get(index + 1);
            if (srcLineNumber != null) {
                sb.append(String.format(formatStr, srcLineNumber));
            } else {
                sb.append(emptyStr);
            }
            sb.append(line).append("\n");
            index++;
        }
        return sb.toString();
    }

}
