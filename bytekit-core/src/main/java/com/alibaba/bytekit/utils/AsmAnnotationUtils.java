package com.alibaba.bytekit.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.alibaba.deps.org.objectweb.asm.tree.AnnotationNode;

/**
 * 
 * @author hengyunabc 2020-05-04
 *
 */
public class AsmAnnotationUtils {

    /**
     * 从注解中查找单个值，即使有重复注解也只返回第一个值
     * 
     * @param <T>
     * @param annotations
     * @param annotationType
     * @param key
     * @return
     */
    public static <T> T queryAnnotationValue(List<AnnotationNode> annotations, String annotationType, String key) {
        if (annotations != null) {
            for (AnnotationNode annotationNode : annotations) {
                if (annotationNode.desc.equals(annotationType)) {
                    if (annotationNode.values != null) {
                        Iterator<Object> iterator = annotationNode.values.iterator();
                        while (iterator.hasNext()) {
                            String name = (String) iterator.next();
                            Object value = iterator.next();
                            if (key.equals(name)) {
                                return (T) value;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 从注解中查找所有的值，如果有重复注解会返回所有值
     * 
     * @param <T>
     * @param annotations
     * @param annotationType
     * @param key
     * @return
     */
    public static <T> List<T> queryAnnotationValues(List<AnnotationNode> annotations, String annotationType,
            String key) {
        List<T> result = new ArrayList<T>();
        if (annotations != null) {
            for (AnnotationNode annotationNode : annotations) {
                if (annotationNode.desc.equals(annotationType)) {
                    if (annotationNode.values != null) {
                        Iterator<Object> iterator = annotationNode.values.iterator();
                        while (iterator.hasNext()) {
                            String name = (String) iterator.next();
                            Object values = iterator.next();
                            if (key.equals(name)) {
                                result.add((T) values);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * <pre>
     * 从注解中查找 数组 字段的值，返回非null 的 List。 比如 `@Test(names = {"abc", "xyz"})` ，返回 List包含
     * "abc", "xyz" 两个元素
     * 
     * <pre>
     * 
     * @param annotations
     * @param annotationType
     * @param key
     * @return
     */
    public static <T> List<T> queryAnnotationArrayValue(List<AnnotationNode> annotations, String annotationType,
            String key) {
        List<T> result = queryAnnotationValue(annotations, annotationType, key);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    public static void addAnnotationInfo(List<AnnotationNode> annotations, String annotationType, String key,
            String value) {

        AnnotationNode annotationNode = null;
        for (AnnotationNode tmp : annotations) {
            if (tmp.desc.equals(annotationType)) {
                annotationNode = tmp;
            }
        }

        if (annotationNode == null) {
            annotationNode = new AnnotationNode(annotationType);
            annotations.add(annotationNode);
        }

        if (annotationNode.values == null) {
            annotationNode.values = new ArrayList<Object>();
        }

        // 查找有没有对应的key
        String name = null;
        List<String> values = null;
        Iterator<Object> iterator = annotationNode.values.iterator();
        while (iterator.hasNext()) {
            if (key.equals(iterator.next())) {
                values = (List<String>) iterator.next();
            } else {
                iterator.next();
            }
        }
        if (values == null) {
            values = new ArrayList<String>();
            annotationNode.values.add(key);
            annotationNode.values.add(values);
        }
        if (!values.contains(values)) {
            values.add(value);
        }
    }
}
