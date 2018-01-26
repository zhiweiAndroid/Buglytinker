# 热修复——Bugly让热修复变得如此简单

# 一、简述

在上一篇[《热修复——Tinker的集成与使用》](https://link.juejin.im/?target=https%3A%2F%2Fjuejin.im%2Fpost%2F5a27bdaf6fb9a044fa19bcfc)中，根据Tinker官方Wiki集成了Tinker，但那仅仅只是本地集成，有一个重要的问题没有解决，那就是补丁从服务器下发到用户手机上，如果你团队中的后台开发人员实力够强，那么完全可以自己做一个补丁管理系统，但我想应该没多少人愿意花精力在这个后台管理系统的开发上面吧，且开发有时候就是在造bug，鬼知道会挖出一个多大的坑呢？对于这样的一个问题，据我所知，市面上有3种Tinker的补丁管理系统，如下：

- [Bugly：热修复](https://link.juejin.im/?target=https%3A%2F%2Fbugly.qq.com)
- [GitHub：tinker-manager](https://link.juejin.im/?target=https%3A%2F%2Fgithub.com%2Fbaidao%2Ftinker-manager)
- [tinkerpatch（Android 热更新服务平台）](https://link.juejin.im/?target=http%3A%2F%2Fwww.tinkerpatch.com%2F)

「Bugly」和「tinker-manager」是免费的，「tinkerpatch」是收费的，因为「tinkerpatch」收费，所以暂时不做考虑。Bugly由腾讯团队开发并维护，稳定性肯定没得说，而「tinker-manager」是GitHub上个人开发者开发维护的，稳定性没法保证（我没有贬低开发者的意思，毕竟势单力薄，人多力量大嘛），故本人觉得，Bugly是目前最优的Tinker热修复解决方案。

# 二、获取App ID

要使用Bugly的热修复功能，首先得注册并登录Bugly，然后[点击进入「Bugly产品页面」](https://link.juejin.im/?target=https%3A%2F%2Fbugly.qq.com%2Fv2%2Fworkbench%2Fapps%2F)，或点击“我的产品 ”。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10ae87d0c6c?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

我这个账号之前是没有创建过产品，所以这里什么也没有，接着点击“新建产品”。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10af2340819?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

填写必要的信息后，点击“保存”。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10af40d4ab9?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

通过“产品设置”，选择刚刚创建的产品（图中第3步），可以查看到产品对应的App ID。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10af246d6c3?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

这个App ID很重要，先记录好，后续会用到。

> Demo的App ID为: 3062edb401。不要用我的，对你来说一点用处都没有，请使用你自己产品的App ID。

# 二、添加插件依赖

项目的build.gradle：

```
dependencies {
    classpath 'com.android.tools.build:gradle:3.0.0'
    // tinkersupport插件(1.0.3以上无须再配置tinker插件）
    classpath "com.tencent.bugly:tinker-support:1.1.1"
}

```

# 三、集成SDK

app的build.gradle：

```
apply from: 'tinker-support.gradle'
android {
    defaultConfig {
		...
        // 开启multidex
        multiDexEnabled true
	}
    // recommend
    dexOptions {
        jumboMode = true
    }
    // 签名配置
    signingConfigs {
        release {
            try {
                storeFile file("./keystore/release.keystore")
                storePassword "testres"
                keyAlias "testres"
                keyPassword "testres"
            } catch (ex) {
                throw new InvalidUserDataException(ex.toString())
            }
        }

        debug {
            storeFile file("./keystore/debug.keystore")
        }
    }

    // 构建类型
    buildTypes {
        release {
            minifyEnabled true
            signingConfig signingConfigs.release
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
        debug {
            debuggable true
            minifyEnabled false
            signingConfig signingConfigs.debug
        }
    }

    sourceSets {
        main {
            jniLibs.srcDirs = ['libs']
        }
    }
}

dependencies {
	...
    implementation "com.android.support:multidex:1.0.1" // 多dex配置
    implementation 'com.tencent.bugly:crashreport_upgrade:1.3.4'// 远程仓库集成方式（推荐）
}

```

签名配置部分请根据你项目的实际情况修改，如：

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10af1b20750?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

# 四、配置Tinker

在app的build.gradle文件同级目录下创建一个tinker-support.gradle文件，内容如下：

```
apply plugin: 'com.tencent.bugly.tinker-support'

def bakPath = file("${buildDir}/bakApk/")

/**
 * 此处填写每次构建生成的基准包目录
 */
def baseApkDir = "tinker-bugly-1211-16-01-34"
def myTinkerId = "base-" + rootProject.ext.android.versionName // 用于生成基准包（不用修改）
//def myTinkerId = "patch-" + rootProject.ext.android.versionName + ".0.0" // 用于生成补丁包（每次生成补丁包都要修改一次，最好是 patch-${versionName}.x.x）

/**
 * 对于插件各参数的详细解析请参考
 */
tinkerSupport {

    // 开启tinker-support插件，默认值true
    enable = true

    // 是否启用加固模式，默认为false.(tinker-spport 1.0.7起支持）
    // isProtectedApp = true

    // 是否开启反射Application模式
    enableProxyApplication = true

    // 是否支持新增非export的Activity（注意：设置为true才能修改AndroidManifest文件）
    supportHotplugComponent = true

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
    tinkerId = "${myTinkerId}"

    // 构建多渠道补丁时使用
    // buildAllFlavorsDir = "${bakPath}/${baseApkDir}"

}

/**
 * 一般来说,我们无需对下面的参数做任何的修改
 * 对于各参数的详细介绍请参考:
 * https://github.com/Tencent/tinker/wiki/Tinker-%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97
 */
tinkerPatch {
    ...
}

```

## 1、overrideTinkerPatchConfiguration

当overrideTinkerPatchConfiguration = true时，tinkerPatch可以省略不写，Bugly会加载默认的Tinker配置。但请注意，如果你的so文件不是存放在libs目录下（与src目录同级），又或者资源文件的存放在你自定义的目录中，那么这时你要小心了，这些文件在制作补丁包时不会被检测，也就是说这些so文件和资源文件将不会被热修复，这种情况下就需要将overrideTinkerPatchConfiguration = false，并设置tinkerPatch的lib和res属性。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10af55d9a95?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

其它具体的配置与说明可以查看[「Tinker-接入指南」](https://link.juejin.im/?target=https%3A%2F%2Fgithub.com%2FTencent%2Ftinker%2Fwiki%2FTinker-%25E6%258E%25A5%25E5%2585%25A5%25E6%258C%2587%25E5%258D%2597)。

## 2、baseApkDir

baseApkDir是基准包（也称基线包）的目录，在生产补丁时需要根据基准包在bakApk下具体文件夹名字修改，如：bakApk/xxxx，到时生成补丁包时要将baseApkDir的值改为xxxx。（xxxx是Tinker自动生成的，根据时间戳来命名）。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10af66b436e?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

## 3、tinkerId

tinkerId是Bugly热修复方案最最重要的一个因素，一般取值为git版本号、versionName等等（我习惯用versionName），它会将补丁包与基准包产生对应关系，假设基准包的tinkerId为 base-1.0，则生成的补丁包中的YAPATCH.MF文件关系如下：

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10af4c517b8?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

Bugly要求baseApk（基准包）的tinkerId与补丁包的tinkerId要不一样。所以，在生成基准包时，请用如下tinkerId：

```
def myTinkerId = "base-" + rootProject.ext.android.versionName // 用于生成基准包（不用修改）

```

当生成补丁包时，请使用如下tinkerId：

```
def myTinkerId = "patch-" + rootProject.ext.android.versionName + ".0.0" // 用于生成补丁包（每次生成补丁包都要修改一次，最好是 patch-${versionName}.x.x）

```

对于同一个基准包，我们可能会多次生成补丁包上传到Bugly的热修复管理后台，这时，这些补丁包的tinkerId也要不一样，不然的话，当客户手机上的App在获取补丁时，会错乱（亲测，当同个基准包的补丁包的tinkerId一样时，App每次重启都会获取不同的补丁包，导致tinkerId相同的补丁包轮流下发）。所以，"patch-" + rootProject.ext.android.versionName + ".0.0"中的".0.0"（称为计数）就是为了区分每次生成的补丁包，如.0.1，.0.2等等，建议versionName更新时计数重置。

> 因为Tinker的配置放在了tinker-support.gradle文件中，与app的build.gradle不在同一个文件中，所以没办法通过android.defaultConfig.versionName直接获取App的versionName，这里我使用了config.gradle来提取共同的属性，rootProject.ext.android.versionName获取的是config.gradle中的versionName属性，详情请百度。

## 4、补丁新旧判定

```
def myTinkerId = "patch-" + rootProject.ext.android.versionName + ".0.0" // 用于生成补丁包（每次生成补丁包都要修改一次，最好是 patch-${versionName}.x.x）

```

对于一个基准包，可以在Bugly上发布多个补丁包（切记tinkerid不同），这里或许会让你误以为计数越大，表明补丁越新，这是错误的，这个计数仅仅只是区分不同的补丁包而已，它没有标记补丁新旧的作用，补丁新旧由Bugly来判定，最后上传的补丁便是最新的补丁，举个例子，我在昨天上传了tinkerid为"patch-1.0.0.9"的补丁1，在今天上传了tinkerid为"patch-1.0.0.1"的补丁2，虽然补丁2的计数比补丁1小，但补丁2比补丁1晚上传，所以补丁2是最新的补丁，即补丁新旧与计数无关。Bugly会下发并应用最新的补丁（即补丁2），但还是建议计数从小到大计算，这里仅仅只是说明Bugly如何判定补丁新旧罢了。

# 五、初始化SDK

Bugly的初始化工作需要在Application中完成，但对原生Tinker来说，默认的Application是无法实现热修复的。看过Tinker官方Wiki的人应该知道，Tinker针对Application无法热修复的问题，给予开发者两个选择，分别是：

- 使用「继承TinkerApplication + DefaultApplicationLike」。
- 使用「DefaultLifeCycle注解 + DefaultApplicationLike」。

这2种选择都需要对自定义的Application进行改造，对于自定义Application代码不多的情况来说还可以接受，但有些情况还是比较"讨厌"这2种选择的，对此，Bugly给出了它的2种解决方法，分别如下：

- 使用原来的自定义Application，Bugly通过反射为App动态生成新的Application。
- 使用「继承TinkerApplication + DefaultApplicationLike」。

> DefaultLifeCycle注解在Bugly中被阉割了。

分别对应tinker-support.gradle文件中enableProxyApplication的值：true或false。

## 1、enableProxyApplication = true

Bugly将通过反射的方式针对项目中自定义的Application动态生成新的Application，下图是源码中的AndroidManifest.xml和编译好的apk中的AndroidManifest.xml：

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10af4eefd0f?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

既然将enableProxyApplication的值设置为true，那接下来的重点就是完成Bugly的初始化工作了。需要在自定义的Application的onCreate()中进行Bugly的配置，在attachBaseContext()中进行Bugly的安装：

```
public class MyApplication extends Application {

    private Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        // 这里实现SDK初始化，appId替换成你的在Bugly平台申请的appId
        // 调试时，将第三个参数改为true
        configTinker();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // you must install multiDex whatever tinker is installed!
        MultiDex.install(mContext);
        // 安装tinker
        // 此接口仅用于反射Application方式接入。
        Beta.installTinker();
    }

}

```

> 注意：
>
> 1. Bugly的安装必须在attachBaseContext()方法中，否则将无法从Bugly服务器获取最新补丁。
> 2. tinker需要你开启MultiDex,你需要在dependencies中进行配置compile "com.android.support:multidex:1.0.1"才可以使用MultiDex.install方法。

最后在清单文件中，声明使用我们自定义的Application即可：

```
<application
    android:name="com.lqr.MyApplication"
    ...>

```

## 2、enableProxyApplication = false

这是Bugly推荐的方式，稳定性有保障（因为第1种方式使用的是反射，可能会存在不稳定的因素），它需要对Application进行改造，首先就是继承TinkerApplication，然后在默认的构造函数中，将第2个参数修改为你项目中的ApplicationLike继承类的全限定名称：

```
public class SampleApplication extends TinkerApplication {
    public SampleApplication() {
        super(ShareConstants.TINKER_ENABLE_ALL, "com.lqr.SampleApplicationLike",
                "com.tencent.tinker.loader.TinkerLoader", false);
    }
}

```

> 注意：这个类集成TinkerApplication类，这里面不做任何操作，所有Application的代码都会放到ApplicationLike继承类当中
> 参数解析
> 参数1：tinkerFlags 表示Tinker支持的类型 dex only、library only or all suuport，default: TINKER_ENABLE_ALL
> 参数2：delegateClassName Application代理类 这里填写你自定义的ApplicationLike
> 参数3：loaderClassName Tinker的加载器，使用默认即可
> 参数4：tinkerLoadVerifyFlag 加载dex或者lib是否验证md5，默认为false

接着就是创建ApplicationLike继承类：

```
public class SampleApplicationLike extends DefaultApplicationLike {

    public static final String TAG = "Tinker.SampleApplicationLike";
    private Application mContext;

    public SampleApplicationLike(Application application, int tinkerFlags,
                                 boolean tinkerLoadVerifyFlag, long applicationStartElapsedTime,
                                 long applicationStartMillisTime, Intent tinkerResultIntent) {
        super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime, applicationStartMillisTime, tinkerResultIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplication();
        configTinker();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onBaseContextAttached(Context base) {
        super.onBaseContextAttached(base);
        // you must install multiDex whatever tinker is installed!
        MultiDex.install(base);
        // 安装tinker
        Beta.installTinker(this);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void registerActivityLifecycleCallback(Application.ActivityLifecycleCallbacks callbacks) {
        getApplication().registerActivityLifecycleCallbacks(callbacks);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Beta.unInit();
    }
}

```

> 注意：
> SampleApplicationLike这个类是Application的代理类，以前所有在Application的实现必须要全部拷贝到这里，在onCreate方法调用SDK的初始化方法，在onBaseContextAttached中调用Beta.installTinker(this)。

最后在清单文件中，声明改造好的Application（注意不是ApplicationLike）：

```
<application
    android:name="com.lqr.SampleApplication"
    ...>

```

## 3、配置Bugly

这是Bugly官方给出的配置，应有尽有，注释也很nice，请仔细看看，对项目的功能拓展与用户体验有帮助：

```
private void configTinker() {
    // 设置是否开启热更新能力，默认为true
    Beta.enableHotfix = true;
    // 设置是否自动下载补丁，默认为true
    Beta.canAutoDownloadPatch = true;
    // 设置是否自动合成补丁，默认为true
    Beta.canAutoPatch = true;
    // 设置是否提示用户重启，默认为false
    Beta.canNotifyUserRestart = true;
    // 补丁回调接口
    Beta.betaPatchListener = new BetaPatchListener() {
        @Override
        public void onPatchReceived(String patchFile) {
            Toast.makeText(mContext, "补丁下载地址" + patchFile, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDownloadReceived(long savedLength, long totalLength) {
            Toast.makeText(mContext,
                    String.format(Locale.getDefault(), "%s %d%%",
                            Beta.strNotificationDownloading,
                            (int) (totalLength == 0 ? 0 : savedLength * 100 / totalLength)),
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDownloadSuccess(String msg) {
            Toast.makeText(mContext, "补丁下载成功", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDownloadFailure(String msg) {
            Toast.makeText(mContext, "补丁下载失败", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onApplySuccess(String msg) {
            Toast.makeText(mContext, "补丁应用成功", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onApplyFailure(String msg) {
            Toast.makeText(mContext, "补丁应用失败", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPatchRollback() {

        }
    };

    // 设置开发设备，默认为false，上传补丁如果下发范围指定为“开发设备”，需要调用此接口来标识开发设备
    Bugly.setIsDevelopmentDevice(mContext, false);
    // 多渠道需求塞入
    // String channel = WalleChannelReader.getChannel(getApplication());
    // Bugly.setAppChannel(getApplication(), channel);
    // 这里实现SDK初始化，appId替换成你的在Bugly平台申请的appId
    Bugly.init(mContext, "e9d0b7f57f", true);
}

```

这里就用到了一开始获取到的App ID了，将其传入Bugly.init()方法的第二个参数，切记，用你自己的App ID。

其中如下两个方法很重要：

- Bugly.setIsDevelopmentDevice()

设置当前设备是不是开发设备，这跟Bugly上传补丁包时所选的"下发范围"有关。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10af8af07d6?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

- Bugly.init(context, appid, isDebug)

这个方法除了设置App ID外，还可以设置是否输出Log，可以观察到Bugly在App启动时做了哪些联网操作。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10af991a9da?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

# 六、AndroidManifest.xml

## 1、 权限配置

```
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.READ_LOGS"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>

```

## 2、Activity配置

```
<activity
    android:name="com.tencent.bugly.beta.ui.BetaActivity"
    android:configChanges="keyboardHidden|orientation|screenSize|locale"
    android:theme="@android:style/Theme.Translucent"/>

```

## 3、FileProvider配置

```
<provider
    android:name="android.support.v4.content.FileProvider"
    android:authorities="${applicationId}.fileProvider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/provider_paths"/>
</provider>

```

如果你使用的第三方库也配置了同样的FileProvider, 可以通过继承FileProvider类来解决合并冲突的问题，示例如下：

```
<provider
    android:name=".utils.BuglyFileProvider"
    android:authorities="${applicationId}.fileProvider"
    android:exported="false"
    android:grantUriPermissions="true"
    tools:replace="name,authorities,exported,grantUriPermissions">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/provider_paths"
        tools:replace="name,resource"/>
</provider>

```

## 4、升级SDK下载路径配置

在res目录新建xml文件夹，创建provider_paths.xml文件如下：

```
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- /storage/emulated/0/Download/${applicationId}/.beta/apk-->
    <external-path name="beta_external_path" path="Download/"/>
    <!--/storage/emulated/0/Android/data/${applicationId}/files/apk/-->
    <external-path name="beta_external_files_path" path="Android/data/"/>
</paths>

```

> 注：1.3.1及以上版本，可以不用进行以上配置，aar已经在AndroidManifest配置了，并且包含了对应的资源文件。

# 七、混淆

```
# Bugly混淆规则
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}

# 避免影响升级功能，需要keep住support包的类
-keep class android.support.**{*;}

```

好了，集成完毕，接下来就是制作基准包、补丁包和上传补丁包了。

# 八、制作基准包

在app编码完成并测试完成后，就是打包上线了，上线前打的包就是基准包啦，下面我们就来制作基准包，分3步：

1. 打开app下的tinker-support.gradle文件。
2. 将带"base"的tinkerId注释解开，并注释掉带"patch"的tinkerId。
3. 双击运行build下的assembleRelease。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10afb085d05?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

> 通常主Module的名字是"app"，但我这个Demo是"tinker-bugly"，所以你执行第3步时，要根据具体项目找到要制作基准包的主Module。

AS在执行assembleRelease指令时，就是在编译基准包了，当编译完成时，app的build目录下会自动生成基准包文件夹，以时间戳来命名的（也就是说，每次执行assembleRelease指令都会在build目录创建不同的基准包文件夹）。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10af83f7635?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

这3个文件对之后制作补丁包来说是相当重要的，你需要做的就是将这3个文件保存好，可以保存到云盘、Git服务器上等等，但就不要让它就这么放着，因为在你执行clean Project时，app的build目录会被删除，这样基准包及mapping与R文件都会丢失。

到这里，你就可以把它（基准包：tinker-bugly-release.apk）上架到应用市场了。试下Demo：

本篇不涉及具体的加固与多渠道打包。

### 1、加固

如果你的app需要加固，那就需要在制作基准包之前，将tinker-support.gradle文件的isProtectedApp = true的注释去掉，然后加固，重新签名，最后上架，它对加固平台也有一定的要求。

详情见[「Bugly热更新使用范例文档最后：加固打包」](https://link.juejin.im/?target=https%3A%2F%2Fbugly.qq.com%2Fdocs%2Fuser-guide%2Finstruction-manual-android-hotfix-demo%2F%23_4)部分。

### 2、多渠道打包

分「gradle配置productFlavors方式」与「多渠道打包工具打多渠道包方式（推荐）」。

详情见[「Bugly热更新使用范例文档：多渠道打包」](https://link.juejin.im/?target=https%3A%2F%2Fbugly.qq.com%2Fdocs%2Fuser-guide%2Finstruction-manual-android-hotfix-demo%2F%23_3)部分。

# 九、补丁包

现在要动态修复App了，对于代码修复、so库修复、资源文件修复，分别对应Demo中的"say something"、"get string from .so"、"我的头像"，修复过程无非是改代码，替换so文件，替换资源文件，这里就不演示了，直接开始制作补丁包，先将tinker-support.gradle文件打开。

## 1、基准包命名

确保基准包及相关文件的命名与配置文件中的一致：

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10afcc1360c?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

## 2、修改baseApkDir与tinkerId

1. 修改baseApkDir的值为基准包所有文件夹的名字。
2. 注释掉带"base"的tinkerId，取消带"patch"的tinkerId的注释（多次生成补丁时，记得修改"计数"，区分不同的补丁）。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10b5a2a61d6?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

## 3、执行编译，生成补丁

打开侧边的Gradle标签，找到项目的主Module，双击tinker-support下的buildTinkerPatchRelease指令，生成补丁包。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10b25a1ce03?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

当编译完成后，在app的build/outputs/patch目录下会在"patch_singed_7zip.apk"文件，它就是补丁包，双击打开它，可以看到其中有一个YAPATCH.MF，里面记录了基准包与补丁包的tinkerId（两者是肯定不同，如果一样则说明配置有问题了）。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10b2988851b?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

# 十、上传补丁包

## 1、流程图解

首先，[点击进入「Bugly产品页面」](https://link.juejin.im/?target=https%3A%2F%2Fbugly.qq.com%2Fv2%2Fworkbench%2Fapps%2F)，或点击“我的产品 ”查看我的产品。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10b2aac1786?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

点击你要管理的产品后，依次点击"应用升级"、"热更新"，可以查看到该产品的补丁下发情况（这个产品我还没上传过补丁，故一片空白）。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10bc77ce13f?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

按下图顺序操作即可上传补丁包：

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10b2891d4e3?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

## 2、上传失败分析

有可能你在上传完补丁包时，页面会提示"未匹配到可应用补丁包的App版本，请确认补丁包的基线版本是否已经发布"。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10bb111e3b0?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

遇到这种情况请先冷静，首先来说明一件事：Bugly怎么知道基线版本是否已经发布？

通常按我们理解的，基准包发布就是上架到应用市场，但应用市场又不会通知Bugly某某产品已经上架了，对吧。其实，Bugly的上架通知是这样的：当基准包在手机上启动时，Bugly框架就会让App联网通知Bugly的服务器，同时上传当前App的版本号、tinkerId等信息，它这么做的目的有如下两个：

- 标记某个tinkerId的基准包已经被安装到手机上使用了（即发布）。
- 获取该tinkerId的基准包最新的补丁信息。

所以，当出现了"未匹配到可应用补丁包的App版本，请确认补丁包的基线版本是否已经发布"这样的提示时，可以确定，这个基准包的tinkerId等信息没有被上传到Bugly服务器，对此，鄙人将踩过的坑总结起来，摸索出了自己的解决方法，分如下几步：

- 检查App是否能够联网。
- 检查App ID是否正确。
- 结合enableProxyApplication的取值，检查AndroidManifest.xml中声明的Application是否写对。
- 检查Bugly的安装是不是在attachBaseContext()或onBaseContextAttached()方法中完成。

像我就犯过这样的错，明明在tinker-support.gradle文件中设置了enableProxyApplication = true，结果在AndroidManifest.xml中却声明了TinkerApplication的继承类。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10b2cf87cce?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

所以这里只需要将AndroidManifest.xml中声明我们自定义的Application即可（MyApplication）。

除了联网问题以外，其他的几种情况都需要重新生成基准包。这里再分享一个可以快速确定App是否有上传过版本信息的方法：

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10bbf6db946?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

## 3、上传成功

先验证下上面的方法，当我把问题解决掉之后，把重新生成的基准包安装到手机上打开（此时Bugly框架会上传App的版本号、tinkerId到服务器），再查看"版本管理"，出现了，版本号为"1.0"（其实就是App的versionName）。

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10b30468e61?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

再回头来看看上传补丁，这次又会有什么不同呢？

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10b2ff5b31a?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

耶，成功。点击"立即下发"，可以看到现在补丁处于"下发中"状态：

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10b30345de0?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

随便来看看用户手中的App是什么反应吧（真正将补丁下发到用户手机上的这段时间可能会有点久，不是立即下发的）：

![(https://user-gold-cdn.xitu.io/2017/12/12/1604a10b370f2eb5?imageslim)回头看看Bugly服务器上的补丁下发情况：

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10b34cfee16?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

# 十一、其他

## 1、补丁管理

Bugly服务器除了可以上传下发补丁外，还可以对补丁进行管理：

![img](https://user-gold-cdn.xitu.io/2017/12/12/1604a10b34eefd84?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

1. 停止下发：不再把该补丁下发到客户手机上（停止后可重新开启）。
2. 撤回：将Bugly服务器上的某个补丁删掉，这个操作是不可逆的（不知道用户手机上被成功打上的补丁是否也会被卸载）。
3. 编辑：可以修改"下发范围"（开发设备、全量设备、备注等等）。
4. 历史：查看修改记录。

## 2、强调一下一些需要注意的地方

1. 一个基准包可以有多个补丁包，Bugly会将最新的补丁进行下发（旧补丁默认会变成"停止下发状态"），客户手机上的App的旧补丁会被新补丁覆盖。
2. 制作基础包时，请使用带"base"的tinkerId，执行的是assembleRelease指令。
3. 制作基础包后，一定要将baseApk、mapping.txt、R.txt保存好，不能弄丢了。
4. 制作补丁包时，先将baseApkDir的值修改为基准包所有文件夹的名字，然后启用带"patch"的tinkerId，同时修改"计数"，执行的是buildTinkerPatchRelease指令。
5. 制作补丁包后，最后打开它检查YAPATCH.MF文件中的from和to信息，检查该补丁包对应的基准包的tinkerId是否正确。
6. 建议上线的基准包将Bugly的Log输出关闭：Bugly.init(mContext, AppID, false);
7. 如果是测试补丁包是否用效果，建议设置为开发设备：Bugly.setIsDevelopmentDevice(mContext, true);
8. so文件需要手动先调用一下 TinkerLoadLibrary.installNavitveLibraryABI(this, CPU_ABI) 方法才能生效。

## 3、Bugly官方文档

- [Bugly Android热更新使用指南](https://link.juejin.im/?target=https%3A%2F%2Fbugly.qq.com%2Fdocs%2Fuser-guide%2Finstruction-manual-android-hotfix%2F%3Fv%3D20170912151050)
- [Bugly Android热更新详解](https://link.juejin.im/?target=https%3A%2F%2Fbugly.qq.com%2Fdocs%2Fuser-guide%2Finstruction-manual-android-hotfix-demo)
- [Bugly Android 热更新常见问题](https://link.juejin.im/?target=https%3A%2F%2Fbugly.qq.com%2Fdocs%2Fuser-guide%2Ffaq-android-hotfix%2F%3Fv%3D20170504092424)
- [热更新API接口](https://link.juejin.im/?target=https%3A%2F%2Fbugly.qq.com%2Fdocs%2Fuser-guide%2Fapi-hotfix%2F%3Fv%3D20170504092424)
- [Bugly多渠道热更新解决方案](https://link.juejin.im/?target=https%3A%2F%2Fbuglydevteam.github.io%2F2017%2F05%2F15%2Fsolution-of-multiple-channel-hotpatch%2F)

## 4、本系列文章链接

- [热修复——深入浅出原理与实现](https://link.juejin.im/?target=https%3A%2F%2Fjuejin.im%2Fpost%2F5a0ad2b551882531ba1077a2)
- [热修复——Tinker的集成与使用](https://link.juejin.im/?target=https%3A%2F%2Fjuejin.im%2Fpost%2F5a27bdaf6fb9a044fa19bcfc)
- [热修复——Bugly让热修复变得如此简单](https://link.juejin.im/?target=https%3A%2F%2Fjuejin.im%2Fpost%2F5a2fa1f26fb9a0450e7616ad)