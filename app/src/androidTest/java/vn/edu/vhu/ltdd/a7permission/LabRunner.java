package vn.edu.vhu.ltdd.a7permission;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Runs the submitted scenarios on the device; no test hooks in the release app. */
public class LabRunner extends Instrumentation {
    private volatile Activity active;
    private final ArrayList<String> results=new ArrayList<>();
    private String mode="full";
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        if(arguments!=null) mode=arguments.getString("mode","full");
        start();
    }
    @Override public void onStart() {
        try {
            ((Application)getTargetContext().getApplicationContext()).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                public void onActivityResumed(Activity a){active=a;}
                public void onActivityCreated(Activity a,Bundle b){active=a;}
                public void onActivityStarted(Activity a){}
                public void onActivityPaused(Activity a){}
                public void onActivityStopped(Activity a){}
                public void onActivitySaveInstanceState(Activity a,Bundle b){}
                public void onActivityDestroyed(Activity a){}
            });
            Intent intent=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            active=startActivitySync(intent); settle();
            android.accessibilityservice.AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();
            info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS | android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            getUiAutomation().setServiceInfo(info);
            execute();
            save("results.txt",android.text.TextUtils.join("\n",results)+"\nALL PASSED\n");
            Bundle done=new Bundle();done.putString("stream","\n"+android.text.TextUtils.join("\n",results)+"\nALL PASSED\n");finish(Activity.RESULT_OK,done);
        } catch(Throwable e) {
            StringWriter sw=new StringWriter();e.printStackTrace(new PrintWriter(sw));
            try { save("results.txt",android.text.TextUtils.join("\n",results)+"\nFAILED\n"+sw); shot("FAILED"); } catch(Throwable ignored){}
            Bundle b=new Bundle();b.putString("stream","FAILED: "+sw);finish(Activity.RESULT_CANCELED,b);
        }
    }
    private void ui(Runnable action) { runOnMainSync(action); waitForIdleSync(); }
    private void pause(long ms) { SystemClock.sleep(ms); }
    private void settle(){waitForIdleSync();pause(900);}
    private View view(int id){return active.findViewById(id);}
    private void hideKeyboard(){ui(()->((InputMethodManager)active.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(active.getWindow().getDecorView().getWindowToken(),0));}
    private void show(int id) {
        hideKeyboard(); ui(()->{View v=view(id);v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true);});pause(300);
    }
    private void click(int id){show(id);ui(()->view(id).performClick());settle();}
    private void edit(int id,String value){ui(()->{
        EditText e=(EditText)view(id);int type=e.getInputType();
        e.setKeyListener(android.text.method.TextKeyListener.getInstance());e.setRawInputType(type);
        e.setText(value);
    });}
    private String text(int id){final String[] s={""};ui(()->s[0]=((TextView)view(id)).getText().toString());return s[0];}
    private String error(int id){final String[] s={""};ui(()->{CharSequence e=((TextView)view(id)).getError();s[0]=e==null?"":e.toString();});return s[0];}
    private void check(String label,boolean pass){
        if(!pass)throw new AssertionError(label);
        results.add("PASS | "+label);Bundle b=new Bundle();b.putString("stream","PASS | "+label+"\n");sendStatus(0,b);
    }
    private void shot(String name)throws Exception{
        hideKeyboard();pause(200);Bitmap b=getUiAutomation().takeScreenshot();
        if(b==null)throw new IOException("Screenshot unavailable");
        File dir=new File(getTargetContext().getExternalFilesDir(null),"evidence");dir.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}
    }
    private void save(String name,String data)throws Exception{
        File dir=new File(getTargetContext().getExternalFilesDir(null),"evidence");dir.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(dir,name))){out.write(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    }
    private Activity waitActivity(String name){
        for(int i=0;i<100;i++){if(active!=null&&active.getClass().getSimpleName().equals(name)){settle();return active;}pause(100);}
        throw new AssertionError("Activity not resumed: "+name);
    }
    private void rotate(boolean landscape){
        Activity old=active;
        ui(()->old.setRequestedOrientation(landscape?ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        for(int i=0;i<80;i++){if(active!=old){settle();return;}pause(100);}
        throw new AssertionError("Rotation did not recreate Activity");
    }
    private void back(){sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();}
    private AccessibilityNodeInfo findNode(String id,String value){
        ArrayList<AccessibilityNodeInfo> roots=new ArrayList<>();
        AccessibilityNodeInfo current=getUiAutomation().getRootInActiveWindow();if(current!=null)roots.add(current);
        for(android.view.accessibility.AccessibilityWindowInfo window:getUiAutomation().getWindows()) {
            AccessibilityNodeInfo root=window.getRoot();if(root!=null)roots.add(root);
        }
        for(AccessibilityNodeInfo root:roots) {
            if(id!=null) {
                List<AccessibilityNodeInfo> list=root.findAccessibilityNodeInfosByViewId(id);
                if(!list.isEmpty())return list.get(0);
                if(id.endsWith("/permission_deny_button")) {
                    list=root.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/permission_deny_and_dont_ask_again_button");
                    if(!list.isEmpty())return list.get(0);
                }
            }
            if(value!=null) {
                for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(value))
                    if(value.equals(String.valueOf(node.getText()))||value.equals(String.valueOf(node.getContentDescription())))return node;
            }
            AccessibilityNodeInfo virtual=scanTree(root,id,value);
            if(virtual!=null)return virtual;
        }
        return null;
    }
    // Compose and remote notification content can expose virtual nodes without findByText indexing.
    private AccessibilityNodeInfo scanTree(AccessibilityNodeInfo node,String id,String value) {
        if(node==null)return null;
        if(id!=null&&id.equals(node.getViewIdResourceName()))return node;
        if(value!=null&&(value.equals(String.valueOf(node.getText()))||value.equals(String.valueOf(node.getContentDescription()))))return node;
        for(int i=0;i<node.getChildCount();i++) {
            AccessibilityNodeInfo match=scanTree(node.getChild(i),id,value);if(match!=null)return match;
        }
        return null;
    }
    private void awaitNode(String id) {
        for(int i=0;i<80;i++){ if(findNode(id,null)!=null)return;pause(200); }
        throw new AssertionError("System window not ready: "+id);
    }
    private void systemClick(String id,String text){
        for(int i=0;i<60;i++){AccessibilityNodeInfo node=findNode(id,text);if(node!=null){
            while(node!=null&&!node.isClickable())node=node.getParent();
            if(node!=null&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK)){settle();return;}
        }pause(150);}
        throw new AssertionError("System control not found: "+id+" / "+text);
    }
    private void shell(String command)throws Exception{
        try(ParcelFileDescriptor fd=getUiAutomation().executeShellCommand(command);FileInputStream in=new FileInputStream(fd.getFileDescriptor())){byte[] b=new byte[2048];while(in.read(b)!=-1){}}
        settle();
    }
    private void execute()throws Exception{
        if(mode.equals("denial")) {

        click(R.id.btnChupAnh);
        systemClick("com.android.permissioncontroller:id/permission_deny_button",null);
        click(R.id.btnChupAnh);shot("03_giai_thich");
        check("A7-T03: first denial shows rationale before asking again",findNode(null,"Vì sao cần quyền này?")!=null);
        systemClick("android:id/button1",null);
        systemClick("com.android.permissioncontroller:id/permission_deny_button",null);shot("04_bi_chan");
        check("A7-T04: second denial offers settings",findNode(null,"Quyền đang bị chặn")!=null);
        systemClick("android:id/button2",null);click(R.id.btnChupAnh);
        check("A7-T04: repeat blocked request shows app settings prompt",findNode(null,"Quyền đang bị chặn")!=null);
        systemClick("android:id/button1",null);shot("05_cai_dat_ung_dung");
        systemClick(null,"Permissions");systemClick(null,"Camera");
        systemClick(null,"Allow only while using the app");shot("05_cap_trong_cai_dat");
        back();back();back();waitActivity("MainActivity");
        check("A7-T05: onResume reflects Settings grant",text(R.id.tvTrangThai).contains("CAMERA: ĐÃ CẤP"));shot("05_onresume_cap_nhat");

            return;
        }

        if(mode.equals("bad_registration")) {
            final String[] actual={""};
            ui(()->{
                try { ((MainActivity)active).registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),value->{}); }
                catch(IllegalStateException e) {actual[0]=e.getMessage();}
            });
            check("A7-analysis: registration while RESUMED throws",actual[0].contains("RESUMED"));
            save("registration_error.txt",actual[0]);return;
        }
        click(R.id.btnChupAnh);awaitNode("com.android.permissioncontroller:id/permission_allow_foreground_only_button");shot("01_hoi_quyen_lan_dau");
        check("A7-T01: first request shows system permission dialog",findNode("com.android.permissioncontroller:id/permission_allow_foreground_only_button",null)!=null);
        if(!mode.equals("demo_grant")) {rotate(true);shot("07_xoay_hop_thoai");}
        systemClick("com.android.permissioncontroller:id/permission_allow_foreground_only_button",null);
        check(mode.equals("demo_grant")?"A7-demo: camera permission granted":"A7-T07: rotated permission callback receives grant",getTargetContext().checkSelfPermission(android.Manifest.permission.CAMERA)==android.content.pm.PackageManager.PERMISSION_GRANTED);
        // The camera is a separate system app. Use its real shutter and confirmation.
        awaitNode("com.android.camera2:id/shutter_button");
        AccessibilityNodeInfo shutter=findNode("com.android.camera2:id/shutter_button",null);
        if(shutter==null) shutter=findNode(null,"Shutter");
        if(shutter==null) shutter=findNode(null,"Take picture");
        if(shutter==null) { shot("camera_controls"); throw new AssertionError("Camera shutter unavailable"); }
        shutter.performAction(AccessibilityNodeInfo.ACTION_CLICK);settle();
        awaitNode("com.android.camera2:id/done_button");
        AccessibilityNodeInfo done=findNode("com.android.camera2:id/done_button",null);
        if(done==null) done=findNode(null,"Done");
        if(done==null) {shot("camera_confirm");throw new AssertionError("Camera confirmation unavailable");}
        done.performAction(AccessibilityNodeInfo.ACTION_CLICK);settle();waitActivity("MainActivity");
        check("A7-T02: actual camera preview Bitmap displayed",text(R.id.tvAnh).contains("pixel"));shot("02_anh_xem_truoc");
        if(mode.equals("demo_grant")) return;
        rotate(false);
        click(R.id.btnThongBao);awaitNode("com.android.permissioncontroller:id/permission_allow_button");shot("06_xin_quyen_thong_bao");
        systemClick("com.android.permissioncontroller:id/permission_allow_button",null);
        check("A7-T06: notification permission granted",getTargetContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED);
        NotificationManager nm=getTargetContext().getSystemService(NotificationManager.class);
        check("A7-T06: channel and real notification created",nm.getNotificationChannel("a7_channel")!=null&&nm.getActiveNotifications().length>0);
        Bitmap screen=getUiAutomation().takeScreenshot();
        shell("input swipe "+(screen.getWidth()/2)+" 1 "+(screen.getWidth()/2)+" "+(screen.getHeight()*3/4)+" 400");
        awaitNode("android:id/title");shot("06_thong_bao");
        systemClick(null,"Xin chào từ Lab A7");waitActivity("MainActivity");
        check("A7-NC3: tapping notification opens app",nm.getActiveNotifications().length==0);shot("NC3_mo_lai_app");
        // Stop here for the first run; denial/settings sequence is a separate fresh install run.

    }
}
