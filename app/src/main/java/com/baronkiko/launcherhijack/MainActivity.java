package com.baronkiko.launcherhijack;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private static Context context;

    String AMAZON_MODEL = Build.MODEL;

    private ListView mListAppInfo;
    private MenuItem launcher, sysApps;
    private int prevSelectedIndex = 0;

    public final static int REQUEST_CODE = 5466;

    public static void SetContext(Context c)
    {
        if (context == null)
            context = c;
    }
    public static Context GetContext()
    {
        return context;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.mainmenu, menu);
        sysApps = menu.getItem(0);
        launcher = menu.getItem(1);
        launcher.setChecked(true);
        sysApps.setChecked(true);
        UpdateList();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.launcher:
                launcher.setChecked(!launcher.isChecked());
                if (launcher.isChecked())
                    sysApps.setChecked(true);
                UpdateList();
                break;

            case R.id.sysApps:
                sysApps.setChecked(!sysApps.isChecked());
                UpdateList();
                break;

            case R.id.help:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sweenwolf/LauncherHijack/blob/master/HELP.md")));
                break;

            case R.id.about:
                startActivity(new Intent(getApplicationContext(), About.class));
                break;

            case  R.id.settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void UpdateList()
    {
        boolean sys = sysApps.isChecked();
        boolean l = launcher.isChecked();
        List<ResolveInfo> appInfo = Utilities.getInstalledApplication(this, l, sys); // Get available apps

        mListAppInfo = findViewById(R.id.lvApps);

        // create new adapter
        AppAdapter adapter = new AppAdapter(this, appInfo, getApplicationContext().getPackageManager());

        // set adapter to list view
        mListAppInfo.setAdapter(adapter);

        SharedPreferences settings = getSharedPreferences("LauncherHijack", MODE_PRIVATE);
        String selectedPackage = settings.getString("ChosenLauncher", "com.teslacoilsw.launcher");

        for (int i = 0; i < appInfo.size(); i++) {
            if (appInfo.get(i).activityInfo.packageName.equals(selectedPackage)) {
                prevSelectedIndex = i;
                mListAppInfo.setSelection(i);
                mListAppInfo.setItemChecked(i, true);
                break;
            }
        }
    }

    private void showSecurityAlert() {
        // Notify User
        String welcomeMessage = "The FireTV devices running Nougat or higher do not provide a UI to the Application Permissions.";
        String welcomeMessage2 = "In order to use this tool on your device, you must first run the below commands from your PC:";
        String adbCommand1 = "# adb tcpip 5555";
        String adbCommand2 = "# adb connect (yourfiretvip)";
        String adbCommand3 = "# adb shell";
        String adbCommand4 = "# pm grant com.baronkiko.launcherhijack android.permission.SYSTEM_ALERT";
        String adbCommand4Part2 = "    _WINDOW";

        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("FireTV Permissions Notice");

        LinearLayout alertContents = new LinearLayout(this);
        LinearLayout.LayoutParams lllp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        TextView alertHeader = new TextView(this);
        TextView alertMessage = new TextView(this);
        TextView alertMessageExtraLine = new TextView(this);

        alertHeader.setText(welcomeMessage + " " + welcomeMessage2 + "\n");
        alertHeader.setGravity(Gravity.CENTER_HORIZONTAL);
        alertHeader.setTextColor(Color.WHITE);

        alertMessage.setText(adbCommand1 + "\n" + adbCommand2 + "\n" + adbCommand3 + "\n" + adbCommand4);
        alertMessage.setGravity(Gravity.LEFT);
        alertMessage.setTextColor(Color.WHITE);

        alertMessageExtraLine.setText(adbCommand4Part2);
        alertMessageExtraLine.setGravity(Gravity.LEFT);
        alertMessageExtraLine.setTextColor(Color.WHITE);

        alertContents.setLayoutParams(lllp);
        alertContents.setOrientation(LinearLayout.VERTICAL);
        alertContents.removeAllViews();
        alertContents.addView(alertHeader);
        alertContents.addView(alertMessage);
        alertContents.addView(alertMessageExtraLine);

        alertDialog.setView(alertContents);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public boolean checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                try {
                    startActivityForResult(intent, REQUEST_CODE);
                }
                catch(SecurityException | ActivityNotFoundException e) {
                    showSecurityAlert();
                }
                return false;
            }
            else {
                return true;
            }
        }
        else {
            return true;
        }
    }

    public static boolean isAccessibilityEnabled(Context context, String id) {

        AccessibilityManager am = (AccessibilityManager) context
                .getSystemService(Context.ACCESSIBILITY_SERVICE);

        List<AccessibilityServiceInfo> runningServices = am
                .getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
        for (AccessibilityServiceInfo service : runningServices) {
            if (id.equals(service.getId())) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SetContext(getApplicationContext());

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        isAccessEnabled();

        mListAppInfo = findViewById(R.id.lvApps);

        // implement event when an item on list view is selected
        mListAppInfo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int pos, long id) {
                // get the list adapter
                AppAdapter appInfoAdapter = (AppAdapter) parent.getAdapter();
                // get selected item on the list
                final ResolveInfo appInfo = (ResolveInfo) appInfoAdapter.getItem(pos);

                // Notify User
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Set Launcher");
                alertDialog.setMessage("Set your launcher to " + appInfo.loadLabel(getPackageManager()) + " (" + appInfo.activityInfo.packageName + ")");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                prevSelectedIndex = pos;

                                // We need an Editor object to make preference changes.
                                // All objects are from android.context.Context
                                SharedPreferences settings = getSharedPreferences("LauncherHijack", MODE_PRIVATE);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString("ChosenLauncher", appInfo.activityInfo.applicationInfo.packageName);
                                editor.putString("ChosenLauncherName", appInfo.activityInfo.name);
                                editor.commit(); // Commit the edits!
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mListAppInfo.setSelection(prevSelectedIndex);
                                mListAppInfo.setItemChecked(prevSelectedIndex, true);
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });

        //Starting service to listen Screen State
        Intent i = new Intent(this,ScreenOnOffService.class);
        i.setAction("com.baronkiko.launcherhijack.ScreenOnOffService");
        startService(i);
    }

    private void isAccessEnabled() {
        if (!isAccessibilityEnabled(context, "com.baronkiko.launcherhijack/.AccServ")) {
            //checking if our device is Firestick 4K
            if ((AMAZON_MODEL.matches("AFTMM"))) {

                String welcomeMessage = "Accessibility Service is disabled for your Firestick 4K.";
                String welcomeMessage2 = "In order to use this tool on your device, you must first run the below commands from your PC:";
                String adbCommand1 = "# adb tcpip 5555";
                String adbCommand2 = "# adb connect (yourfiretvip)";
                String adbCommand3 = "# adb shell";
                String adbCommand4 = "# pm grant com.baronkiko.launcherhijack android.permission.WRITE_SECURE_SETTINGS";
                String cmddone = "Press Done only after giving the permissions";

                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle("Accessibility Service Disabled")
                        .setMessage(welcomeMessage + "\n" + welcomeMessage2 + "\n" + adbCommand1 +
                                "\n" + adbCommand2 + "\n" + adbCommand3 + "\n" + adbCommand4 + "\n" + cmddone)
                        .setCancelable(true)
                        .setNeutralButton("Help", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sweenwolf/LauncherHijack/blob/master/HELP.md")));
                                isAccessEnabled();
                            }
                        })
                        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MainActivity.this, "Launcher Hijack will not work", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        })
                        .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setSecurePerm();
                            }
                        });


                AlertDialog alert = builder.create();
                alert.show();

            } else {

                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle("Accessibility Service Disabled")
                        .setMessage("Accessibility Service is disabled. You must enable it to ensure Launcher Hijack's functionality")
                        .setCancelable(true)
                        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                if (!SettingsMan.GetSettings().RunningOnTV) {
                    builder.setNeutralButton("Open Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            startActivity(intent);
                        }
                    });
                }
                AlertDialog alert = builder.create();
                alert.show();
            }
        } else if (getApplicationContext().getSharedPreferences("LauncherHijack", MODE_PRIVATE).getString("ChosenLauncher", "com.baronkiko.launcherhijack").equals("com.baronkiko.launcherhijack"))
            Toast.makeText(getApplicationContext(), "Please select a launcher", Toast.LENGTH_LONG).show();

    }

    private void setSecurePerm() {
        //checking for Secure Permissions
        int SecurePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS);
        if (SecurePerm == 0) {
            try {
                Settings.Secure.putString(getContentResolver(), "enabled_accessibility_services", "com.baronkiko.launcherhijack/com.baronkiko.launcherhijack.AccServ");
                Settings.Secure.putString(getContentResolver(), "accessibility_enabled", "1");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getApplicationContext(), "SECURE Permission not Granted", Toast.LENGTH_SHORT).show();
            isAccessEnabled();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            if (Settings.canDrawOverlays(this)) {
                ServiceMan.Start(this);
            }
        }
    }
}
