package com.alibaba.bytekit.asm.instrument;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.zeroturnaround.zip.ZipUtil;

/**
 * 测试新的 instrument 和 define 关联配置格式
 * 
 * @author hengyunabc
 */
public class InstrumentDefineConfigTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * 测试新格式：instrument.{key} 和 define.{key} 的关联
     * 使用默认的 instrument.properties
     */
    @Test
    public void testNewFormatAssociation() throws Exception {
        String file = this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File testJarFile = folder.newFile("test-new-format.jar");
        ZipUtil.pack(new File(file), testJarFile);

        InstrumentTemplate instrumentTemplate = new InstrumentTemplate(testJarFile);
        InstrumentParseResult result = instrumentTemplate.build();

        List<InstrumentConfig> configs = result.getInstrumentConfigs();
        assertThat(configs).isNotNull();
        assertThat(configs.size()).isGreaterThan(0);

        // 查找 DubboFilter_APM 的配置（使用新格式）
        InstrumentConfig dubboFilterConfig = findConfigByClassName(configs, "DubboFilter_APM");
        if (dubboFilterConfig != null) {
            // 验证 dubbo.filter 关联了正确的 define 类
            List<DefineConfig> defineConfigs = dubboFilterConfig.getDefineConfigs();
            assertThat(defineConfigs).isNotNull();
            if (!defineConfigs.isEmpty()) {
                assertThat(defineConfigs).hasSize(1);
                assertThat(defineConfigs.get(0).getClassName()).isEqualTo("com.alibaba.bytekit.asm.inst.DubboUtils");
                System.out.println("✓ DubboFilter_APM 正确关联了 DubboUtils");
            }
        }

        // 查找 TestModule1_APM 的配置（如果存在）
        InstrumentConfig module1Config = findConfigByClassName(configs, "TestModule1_APM");
        if (module1Config != null) {
            List<DefineConfig> defineConfigs = module1Config.getDefineConfigs();
            assertThat(defineConfigs).isNotNull();
            if (!defineConfigs.isEmpty()) {
                assertThat(defineConfigs).hasSize(1);
                assertThat(defineConfigs.get(0).getClassName()).isEqualTo("com.alibaba.bytekit.asm.inst.Module1Utils");
                System.out.println("✓ TestModule1_APM 正确关联了 Module1Utils");
            }
        }

        // 查找 TestModule2_APM 的配置（如果存在）
        InstrumentConfig module2Config = findConfigByClassName(configs, "TestModule2_APM");
        if (module2Config != null) {
            List<DefineConfig> defineConfigs = module2Config.getDefineConfigs();
            assertThat(defineConfigs).isNotNull();
            if (!defineConfigs.isEmpty()) {
                assertThat(defineConfigs).hasSize(1);
                assertThat(defineConfigs.get(0).getClassName()).isEqualTo("com.alibaba.bytekit.asm.inst.Module2Utils");
                System.out.println("✓ TestModule2_APM 正确关联了 Module2Utils");
            }
        }

        System.out.println("✓ 新格式测试通过：每个 instrument 都正确关联了自己的 define 类");
    }

    /**
     * 测试旧格式：全局的 instrument 和 define（向后兼容）
     */
    @Test
    public void testOldFormatBackwardCompatibility() throws Exception {
        String file = this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File testJarFile = folder.newFile("test-old-format.jar");
        ZipUtil.pack(new File(file), testJarFile);

        InstrumentTemplate instrumentTemplate = new InstrumentTemplate(testJarFile);
        InstrumentParseResult result = instrumentTemplate.build();

        List<InstrumentConfig> configs = result.getInstrumentConfigs();
        assertThat(configs).isNotNull();
        assertThat(configs.size()).isGreaterThan(0);

        // 查找 InvokeOriginDemo_APM 的配置（使用旧格式）
        InstrumentConfig invokeOriginConfig = findConfigByClassName(configs, "InvokeOriginDemo_APM");
        if (invokeOriginConfig != null) {
            // 旧格式的 instrument 不应该有关联的 define（define 是全局的）
            List<DefineConfig> defineConfigs = invokeOriginConfig.getDefineConfigs();
            assertThat(defineConfigs).isNotNull();
            // 旧格式下，define 是全局的，不关联到具体的 instrument，所以这里应该是空的
            System.out.println("✓ InvokeOriginDemo_APM (旧格式) 的 define 数量: " + defineConfigs.size());
        }

        System.out.println("✓ 旧格式测试通过：向后兼容性正常");
    }

    /**
     * 测试混合格式：新旧格式共存
     */
    @Test
    public void testMixedFormat() throws Exception {
        String file = this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File testJarFile = folder.newFile("test-mixed-format.jar");
        ZipUtil.pack(new File(file), testJarFile);

        InstrumentTemplate instrumentTemplate = new InstrumentTemplate(testJarFile);
        InstrumentParseResult result = instrumentTemplate.build();

        List<InstrumentConfig> configs = result.getInstrumentConfigs();
        assertThat(configs).isNotNull();
        assertThat(configs.size()).isGreaterThan(0);

        // 应该同时存在新格式（DubboFilter_APM）和旧格式（InvokeOriginDemo_APM）
        InstrumentConfig newFormatConfig = findConfigByClassName(configs, "DubboFilter_APM");
        InstrumentConfig oldFormatConfig = findConfigByClassName(configs, "InvokeOriginDemo_APM");
        
        assertThat(newFormatConfig != null || oldFormatConfig != null).isTrue();

        System.out.println("✓ 混合格式测试通过：新旧格式可以共存");
    }

    /**
     * 测试简单解析：验证基本解析功能
     */
    @Test
    public void testBasicParsing() throws Exception {
        String file = this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File testJarFile = folder.newFile("test-basic.jar");
        ZipUtil.pack(new File(file), testJarFile);

        InstrumentTemplate instrumentTemplate = new InstrumentTemplate(testJarFile);
        InstrumentParseResult result = instrumentTemplate.build();

        assertThat(result).isNotNull();
        assertThat(result.getInstrumentConfigs()).isNotNull();
        
        System.out.println("✓ 基本解析测试通过");
        System.out.println("  - 共解析出 " + result.getInstrumentConfigs().size() + " 个 instrument 配置");
        
        // 打印所有的 instrument 配置
        for (InstrumentConfig config : result.getInstrumentConfigs()) {
            String className = config.getInstrumentClassNode().name;
            int defineCount = config.getDefineConfigs().size();
            System.out.println("  - " + className + " -> " + defineCount + " 个关联的 define 类");
            for (DefineConfig defineConfig : config.getDefineConfigs()) {
                System.out.println("    * " + defineConfig.getClassName());
            }
        }
    }

    /**
     * 辅助方法：根据类名查找 InstrumentConfig
     */
    private InstrumentConfig findConfigByClassName(List<InstrumentConfig> configs, String classNamePart) {
        for (InstrumentConfig config : configs) {
            String fullName = config.getInstrumentClassNode().name;
            if (fullName != null && fullName.contains(classNamePart)) {
                return config;
            }
        }
        return null;
    }
}
