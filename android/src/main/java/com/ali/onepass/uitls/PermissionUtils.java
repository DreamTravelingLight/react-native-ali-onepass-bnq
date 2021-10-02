package com.ali.onepass.uitls;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
//import android.support.v4.content.PermissionChecker;

import java.util.ArrayList;
import java.util.List;

import androidx.core.content.PermissionChecker;

public class PermissionUtils {
    @TargetApi(Build.VERSION_CODES.M)
    public static void checkAndRequestPermissions(Activity context, int requestCode,
                                                  String... permissions) {
        List<String> deniedPermissions = new ArrayList<>(permissions.length);
        for(String permission:permissions) {
            if(PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_DENIED) {
                deniedPermissions.add(permission);
            }
        }
        if(!deniedPermissions.isEmpty()) {
            String[] ps = new String[deniedPermissions.size()];
            ps = deniedPermissions.toArray(ps);
            context.requestPermissions(ps, requestCode);
        }
    }
}
