package com.zlb.utils.ntp;

import android.os.SystemClock;
import android.util.Log;

import com.zlb.Sp.SPDao;
import com.zlb.Sp.SPKey;
import com.zlb.base.BaseApplication;
import com.zlb.httplib.scheduler.SwitchSchedulers;

import javax.inject.Inject;

import es.dmoral.toasty.Toasty;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * 时间校准Utils.可以自由配置多个外部的NTP 服务器
 */
public class SyncNtpTimeUtils {

    @Inject
    SPDao spDao;

    /**
     * NTP 服务器的地址自己找稳定的配置在这里；
     */
    private String[] ntpServerHost = new String[]{
            "time.apple.com",
            "ntp3.aliyun.com",
            "cn.pool.ntp.org"
    };

    @Inject
    public SyncNtpTimeUtils() {
    }

    /**
     * 从NTP 服务器获取时间
     */
    private long getTimeFromNtpServer(String ntpHost) {
        SntpClient client = new SntpClient();
        boolean isSuccessful = client.requestTime(ntpHost, 3000);
        if (isSuccessful) {
            return client.getNtpTime();
        }
        return -1;
    }

    /**
     * 多个通道一起
     */
    public void syncNTPTime() {

        Observable.create((ObservableOnSubscribe<Long>) emitter -> {
            for (int i = 0; i < ntpServerHost.length; i++) {
                long time = getTimeFromNtpServer(ntpServerHost[i]);
                if (time != -1) {
                    emitter.onNext(time);
                    break;
                }
                if ((i == ntpServerHost.length - 1) && (time == -1)) {
                    emitter.onError(null);
                }
            }
        })
                .compose(SwitchSchedulers.applySchedulers())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Long o) {
                        spDao.saveData(SPKey.KEY_SOME_BOOTED_TIME, SystemClock.elapsedRealtime());
                        spDao.saveData(SPKey.KEY_SOME_SERVER_TIME, o.longValue());

                        Log.e("TIME", o.longValue() + "---" + (System.currentTimeMillis() - o.longValue()));
//                        Toasty.info(BaseApplication.getAppContext(), o.longValue() + " time").show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("TIME", e.toString() + "---");
                        Toasty.info(BaseApplication.getAppContext(), "very time error" + e.toString()).show();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


}