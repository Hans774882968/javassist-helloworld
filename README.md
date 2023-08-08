[toc]

## 引言

之前研究[JEB 3.19逆向方案](https://blog.csdn.net/hans774882968/article/details/122935187)的时候看到 Java Agent + javassist 可以实现方法 hook 。在此展开进一步研究。

使用的jar文件：`D:\\java-source-codes\\jeb.jar`。`com.pnfsoftware.jeb.client.Licensing.getBuildTypeString`会调用`com.pnfsoftware.jebglobal.sz.RF(byte[], int, int)`。`sz.RF`是解码常量字符串的方法，属于JEB常量字符串隐藏方案的一部分。

项目创建：`Maven for Java`插件，`ctrl+shift+P`，`Maven: Create Maven Project`。模板项目输入`quickstart`来搜索。

## ReadJarAndPatch.java：尝试 hook 后写入文件

[传送门](./src/main/java/com/example/hook_jeb_jar/ReadJarAndPatch.java)

要点：

1. 调用`ClassPool.getDefault().insertClassPath(jarFilePath);`后即可开始 hook 。
2. 正常 hook `sz.RF()`，但需要调用`cls.toClass()`。
3. 通过`jarUrlClassLoader`加载`licensingClass1`后，发现`sz.RF()`能够 hook 成功。但调用`cls.toClass()`获取`licensingClass2`后发现会报错`Exception in thread "main" java.lang.NoClassDefFoundError: com/pnfsoftware/jeb/util/logging/GlobalLog`。

## HookJeb.java：成功hook JEB

传送门：`src\main\java\com\example\hook_jeb_jar\HookJeb.java`

新增依赖：
- lombok：为了使用Slf4j
- logback：为了使用Slf4j

新增`MANIFEST.MF`。注意最后一行需要有一个空行，否则无法解析最后一行的配置。

```yaml
Manifest-Version: 1.0
Premain-Class: com.example.hook_jeb_jar.HookJeb
Can-Redefine-Classes: true
```

`pom.xml`配置`maven-assembly-plugin`：

```xml
    <plugins>
      <plugin>
        <artifactId>maven-assembly-plugin</artifactId>
        <configuration>
          <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
          </descriptorRefs>
          <archive>
            <manifestFile>src/main/META-INF/MANIFEST.MF</manifestFile>
          </archive>
        </configuration>
        <executions>
          <execution>
            <id>make-assembly</id>
            <phase>package</phase>
            <goals>
              <goal>single</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
```

VSCode点击左下角`MAVEN > Lifecycle > package`即可打包。我们需要的是`maven-assembly-plugin`生成的`hook-jeb-jar-1.0-SNAPSHOT-jar-with-dependencies.jar`，另一个jar忽略。

启动JEB命令：

```bash
java -javaagent:C:\Users\admin\Desktop\hook-jeb-jar-1.0-SNAPSHOT-jar-with-dependencies.jar -jar <JEB3.19安装路径>\bin\app\jebc.jar
```

## PatchExpirationTest.java：hook后成功实现主动调用

传送门：`src\main\java\com\example\hook_jeb_jar\PatchExpirationTest.java`

`.vscode\launch.json`配置：

```json
        {
            "type": "java",
            "name": "PatchExpirationTest",
            "request": "launch",
            "mainClass": "com.example.hook_jeb_jar.PatchExpirationTest",
            "projectName": "hook-jeb-jar",
            "vmArgs": "-javaagent:\"<hook-jeb-jar-1.0-SNAPSHOT-jar-with-dependencies.jar所在路径>\"=shouldInsertClassPathJeb"
        }
```