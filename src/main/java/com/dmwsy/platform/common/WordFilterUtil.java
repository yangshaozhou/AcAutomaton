package com.dmwsy.platform.common;

import java.io.File;

public class WordFilterUtil {
    private static XEyes ge = new XEyes();

    /**
     * 从类路径下获取敏感词并创建AC自动机
     */
    public static void initGoldenEyes() {
        ge = new XEyes();
        String path = WordFilterUtil.class.getResource("/").getPath() + "/conf/sensitive.txt";
        File file = new File(path);
        ge.indexSensitiveFromFile(file);
        System.out.println("reload configure " + file.getAbsolutePath());
    }

    /**
     * 其对上面方法的封装
     * @return
     */
    public static XEyes getGoldenEyes() {
        initGoldenEyes();
        return ge;
    }
}
