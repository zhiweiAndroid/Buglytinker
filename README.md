# Bugly让热修复变得如此简单

1. ### 一、配置参数

   - **添加插件依赖**

     工程根目录下“build.gradle”文件中添加：

     ```
     buildscript {
         repositories {
             jcenter()
         }
         dependencies {
             // tinkersupport插件，其中latest.release指代最新版本号，也可以指定明确的版本号，例如1.0.3
             classpath "com.tencent.bugly:tinker-support:latest.release"
         }
     }123456789
     ```

   ### 二、集成SDK

   - **gradle配置**

     在app module的“build.gradle”文件中添加（示例配置）：

     ```
     dependencies {
         compile 'com.tencent.bugly:crashreport_upgrade:1.3.4'
         compile 'com.tencent.bugly:nativecrashreport:latest.release'
         //其中latest.release指代最新版本号，也可以指定明确的版本号，例如2.2.0
     }
     ```

     在app module的“build.gradle”文件中添加：

     ```
     // 依赖插件脚本
     apply from: 'tinker-support.gradle'
     ```

     #### app module的"build.gradle"完整代码

     ~~~Java
     apply plugin: 'com.android.application'
     // 依赖插件脚本
     apply from: 'tinker-support.gradle'
     android {

         compileSdkVersion 26
         buildToolsVersion "26.0.2"
         defaultConfig {
             applicationId "com.sina.buglytinkerdemo"
             minSdkVersion 16
             targetSdkVersion 26
             versionCode 100
             versionName "1.0.0"
             testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
             multiDexEnabled true
             ndk {
                 //设置支持的SO库架构
                 abiFilters 'armeabi' //, 'x86', 'armeabi-v7a', 'x86_64', 'arm64-v8a'
             }
         }


         dexOptions {
             jumboMode = true
         }

         signingConfigs {
             release {
                 keyAlias 'tinker'
                 keyPassword '123456'
                 storeFile file('../bugly.jks')
                 storePassword '123456'
             }
         }

         buildTypes {
             release {
                 minifyEnabled false
                 signingConfig signingConfigs.release
                 proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
             }
         }

         sourceSets {
             main {
                 jniLibs.srcDirs = ['libs']
             }
         }
     }

     dependencies {
         compile fileTree(include: ['*.jar'], dir: 'libs')
         androidTestCompile('com.android.support.test.espresso:espresso-core:2.2.2', {
             exclude group: 'com.android.support', module: 'support-annotations'
         })
         testCompile 'junit:junit:4.12'
         compile 'com.android.support:appcompat-v7:26.0.0-alpha1'
         compile "com.android.support:multidex:1.0.1" // 多dex配置
         //注释掉原有bugly的仓库
         //compile 'com.tencent.bugly:crashreport:latest.release'//其中latest.release指代最新版本号，也可以指定明确的版本号，例如2.3.2
         //compile 'com.tencent.bugly:crashreport:latest.release'//其中latest.release指代最新版本号，也可以指定明确的版本号，例如1.3.4
         compile 'com.tencent.bugly:crashreport_upgrade:1.3.4'
         compile 'com.tencent.bugly:nativecrashreport:latest.release' //其中latest.release指代最新版本号，也可以指定明确的版本号，例如2.2.0

     }
     ~~~

   - tinker-support.gradle**内容如下所示（示例配置）：

     *需要在同级目录下创建tinker-support.gradle这个文件*

     ```java
     apply plugin: 'com.tencent.bugly.tinker-support'

     def bakPath = file("${buildDir}/bakApk/")

     /**
      * 此处填写每次构建生成的基准包目录
      */
     def baseApkDir = "app-0126-16-47-47"

     /**
      * 对于插件各参数的详细解析请参考
      */
     tinkerSupport {

         // 开启tinker-support插件，默认值true
         enable = true

         // 指定归档目录，默认值当前module的子目录tinker
         autoBackupApkDir = "${bakPath}"

         // 是否启用覆盖tinkerPatch配置功能，默认值false
         // 开启后tinkerPatch配置不生效，即无需添加tinkerPatch
         overrideTinkerPatchConfiguration = true

         // 编译补丁包时，必需指定基线版本的apk，默认值为空
         // 如果为空，则表示不是进行补丁包的编译
         // @{link tinkerPatch.oldApk }
         baseApk = "${bakPath}/${baseApkDir}/app-release.apk"

         // 对应tinker插件applyMapping
         baseApkProguardMapping = "${bakPath}/${baseApkDir}/app-release-mapping.txt"

         // 对应tinker插件applyResourceMapping
         baseApkResourceMapping = "${bakPath}/${baseApkDir}/app-release-R.txt"

         // 构建基准包和补丁包都要指定不同的tinkerId，并且必须保证唯一性
          // tinkerId = "1.0.0-base"
         tinkerId = "1.0.0-patch"

         // 构建多渠道补丁时使用
         // buildAllFlavorsDir = "${bakPath}/${baseApkDir}"

         // 是否启用加固模式，默认为false.(tinker-spport 1.0.7起支持）
         // isProtectedApp = true

         // 是否开启反射Application模式
         enableProxyApplication = false

         // 是否支持新增非export的Activity（注意：设置为true才能修改AndroidManifest文件）
         supportHotplugComponent = true

     }

     /**
      * 一般来说,我们无需对下面的参数做任何的修改
      * 对于各参数的详细介绍请参考:
      * https://github.com/Tencent/tinker/wiki/Tinker-%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97
      */
     tinkerPatch {
         //oldApk ="${bakPath}/${appName}/app-release.apk"
         ignoreWarning = false
         useSign = true
         dex {
             dexMode = "jar"
             pattern = ["classes*.dex"]
             loader = []
         }
         lib {
             pattern = ["lib/*/*.so"]
         }

         res {
             pattern = ["res/*", "r/*", "assets/*", "resources.arsc", "AndroidManifest.xml"]
             ignoreChange = []
             largeModSize = 100
         }

         packageConfig {
         }
         sevenZip {
             zipArtifact = "com.tencent.mm:SevenZip:1.1.10"
     //        path = "/usr/local/bin/7za"
         }
         buildConfig {
             keepDexApply = false
             //tinkerId = "1.0.1-base"
             //applyMapping = "${bakPath}/${appName}/app-release-mapping.txt" //  可选，设置mapping文件，建议保持旧apk的proguard混淆方式
             //applyResourceMapping = "${bakPath}/${appName}/app-release-R.txt" // 可选，设置R.txt文件，通过旧apk文件保持ResId的分配
         }
     }
     ```

   ### 三、初始化SDK

   **enableProxyApplication = false 的情况**

   - 自定义Application

   ```java
   public class SampleApplication extends TinkerApplication {
       public SampleApplication() {
           super(ShareConstants.TINKER_ENABLE_ALL, "com.yiba.test.buglypatch.SampleApplicationLike",
                   "com.tencent.tinker.loader.TinkerLoader", false);
       }
   }
   ```

   - 自定义ApplicationLike

   ```Java
   public class SampleApplicationLike extends DefaultApplicationLike {

       public static final String TAG = "Tinker.SampleApplicationLike";

       public SampleApplicationLike(Application application, int tinkerFlags,
               boolean tinkerLoadVerifyFlag, long applicationStartElapsedTime,
               long applicationStartMillisTime, Intent tinkerResultIntent, Resources[] resources,
               ClassLoader[] classLoader, AssetManager[] assetManager) {
           super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime,
                   applicationStartMillisTime, tinkerResultIntent, resources, classLoader,
                   assetManager);
       }


       @Override
       public void onCreate() {
           super.onCreate();
           // 这里实现SDK初始化，appId替换成你的在Bugly平台申请的appId
           Bugly.init(getApplication(), "b11af4c406", true);
       }


       @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
       @Override
       public void onBaseContextAttached(Context base) {
           super.onBaseContextAttached(base);
           // you must install multiDex whatever tinker is installed!
           MultiDex.install(base);

           // 安装tinker
           // TinkerManager.installTinker(this); 替换成下面Bugly提供的方法
           Beta.installTinker(this);
       }

       @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
       public void registerActivityLifecycleCallback(Application.ActivityLifecycleCallbacks callbacks) {
           getApplication().registerActivityLifecycleCallbacks(callbacks);
       }

   }
   ```

   ### 四、AndroidManifest.xml配置

   - 权限配置

   ```java
   <uses-permission android:name="android.permission.READ_PHONE_STATE" />
   <uses-permission android:name="android.permission.INTERNET" />
   <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
   <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
   <uses-permission android:name="android.permission.READ_LOGS" />
   <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
   <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
   ```

   - Activity配置

   ```Java
   <activity
       android:name="com.tencent.bugly.beta.ui.BetaActivity"
       android:theme="@android:style/Theme.Translucent" />
   ```

   - 配置FileProvider（Android N之后配置）

   ### 五、混淆配置(本文未混淆所以未生成mapping.txt)

   如需混淆,如下 :为了避免混淆SDK，在Proguard混淆文件中增加以下配置：

   ```
   -dontwarn com.tencent.bugly.**
   -keep public class com.tencent.bugly.**{*;}
   ```

   如果你使用了support-v4包，你还需要配置以下混淆规则：

   ```
   -keep class android.support.**{*;}
   ```

   ------

   ### 六、编译基准包

   基准包就是原先运行有bug的包。

   点击Android Studio右上角的Gradle按钮，找到项目的assembleRelease任务，双击执行assembleRelease任务。

   ![Image text](https://github.com/zhiweiAndroid/Buglytinker/blob/master/demopicture/1.png)

   任务执行完成后，会在build的目录下生成如下文件：

   ![Image text](https://github.com/zhiweiAndroid/Buglytinker/blob/master/demopicture/2.png)

   ### 七、修复基准版代码

   修复前代码：

   ```java
      @Override
       protected void onCreate(Bundle savedInstanceState) {
           super.onCreate(savedInstanceState);
           setContentView(R.layout.activity_main);
           findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                   String a =null;
                   if (a.equals("a")){
                       Toast.makeText(MainActivity.this,"我是吐司",Toast.LENGTH_SHORT).show();
                   }
               }
           });
       }
   ```

   ------

   修复后代码:

   ```java
   @Override
       protected void onCreate(Bundle savedInstanceState) {
           super.onCreate(savedInstanceState);
           setContentView(R.layout.activity_main);
           findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                   String a ="a";
                   if (a.equals("a")){
                       Toast.makeText(MainActivity.this,"我是吐司",Toast.LENGTH_SHORT).show();
                   }
               }
           });
       }
   ```

   ### 八、生成补丁包

   - 修改appName名称以及文件名称都需要保持一致。同时将基准包的tinkerid=1.0.0-base改为补丁包的tinkerid=1.0.0-patch。

   ![Image text](https://github.com/zhiweiAndroid/Buglytinker/blob/master/demopicture/3.png)

   - **生成补丁包**

   执行生成补丁包的任务操作,任务执行完之后，会生成3个文件，其中patch_signed_7zip.apk是我们需要的补丁包

   ![Image text](https://github.com/zhiweiAndroid/Buglytinker/blob/master/demopicture/4.png)

   生成patch_signed_7zip.apk包后,点击检测版本

   ![Image text](https://github.com/zhiweiAndroid/Buglytinker/blob/master/demopicture/5.png)

   ### 将补丁包上传至bugly的应用升级模块的热更新中

   ------

   *上传了补丁包可能不会立马生效(稍等一会儿,我等了5分钟左右)，多试几次就好了*

- [Bugly Android热更新使用指南](https://link.juejin.im/?target=https%3A%2F%2Fbugly.qq.com%2Fdocs%2Fuser-guide%2Finstruction-manual-android-hotfix%2F%3Fv%3D20170912151050)
- [Bugly Android热更新详解](https://link.juejin.im/?target=https%3A%2F%2Fbugly.qq.com%2Fdocs%2Fuser-guide%2Finstruction-manual-android-hotfix-demo)
- [Bugly Android 热更新常见问题](https://link.juejin.im/?target=https%3A%2F%2Fbugly.qq.com%2Fdocs%2Fuser-guide%2Ffaq-android-hotfix%2F%3Fv%3D20170504092424)
- [热更新API接口](https://link.juejin.im/?target=https%3A%2F%2Fbugly.qq.com%2Fdocs%2Fuser-guide%2Fapi-hotfix%2F%3Fv%3D20170504092424)
- [Bugly多渠道热更新解决方案](https://link.juejin.im/?target=https%3A%2F%2Fbuglydevteam.github.io%2F2017%2F05%2F15%2Fsolution-of-multiple-channel-hotpatch%2F)

## 4、本系列文章链接

- [热修复——深入浅出原理与实现](https://link.juejin.im/?target=https%3A%2F%2Fjuejin.im%2Fpost%2F5a0ad2b551882531ba1077a2)
- [热修复——Tinker的集成与使用](https://link.juejin.im/?target=https%3A%2F%2Fjuejin.im%2Fpost%2F5a27bdaf6fb9a044fa19bcfc)
- [热修复——Bugly让热修复变得如此简单](https://link.juejin.im/?target=https%3A%2F%2Fjuejin.im%2Fpost%2F5a2fa1f26fb9a0450e7616ad)