package com.sina.buglytinkerdemo;

import com.tencent.tinker.loader.app.TinkerApplication;
import com.tencent.tinker.loader.shareutil.ShareConstants;

public class SampleApplication extends TinkerApplication {
    public SampleApplication() {
        super(ShareConstants.TINKER_ENABLE_ALL, "com.sina.buglytinkerdemo.SampleApplicationLike",
                "com.tencent.tinker.loader.TinkerLoader", false);
    }
}