package org.apache.cordova.plugin.ipfetcher;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
* This class echoes a string called from JavaScript.
*/
public class IPFetcher extends CordovaPlugin {


    private static final String LOG_TAG = "NetworkCrawler";
    private static final int TIMEOUT = 500;

    public static final ArrayList<String> deviceList = new ArrayList<String>();

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("fetchip")) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        final long start = System.currentTimeMillis();

                        ExecutorService es = Executors.newFixedThreadPool(500);

                        WifiManager wm = (WifiManager) IPFetcher.this.cordova.getActivity().getSystemService(Context.WIFI_SERVICE);
                        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                        String subnet = ip.substring(0, ip.lastIndexOf("."));
                        Log.i("NetworkCrawler", "Crawling subnet: " + subnet);
                        crawlSubnet(es, subnet);
    //                    for (int n = 1; n < 255; n++) {
    //                        NetworkCrawler.crawlSubnet(es, "192.168." + n);
    //                    }
                        es.shutdown();

                        boolean finshed = es.awaitTermination(2, TimeUnit.MINUTES);
                        Log.i("NetworkCrawler", "Execution time: " + ((System.currentTimeMillis() - start) / 1000));

                        Log.i("NetworkCrawler", "DEVICE FOUND: " + TextUtils.join("|", deviceList));
                        callbackContext.success(TextUtils.join("|", deviceList));
                    } catch (Exception e) {
                        Log.e("NetworkCrawler", "Upps", e);
                        callbackContext.error("Something went wrong");
                    }
                }
            };
            new Thread(r).start();
        }
        return true;
    }

    public static void crawlSubnet(ExecutorService es, String subnet) {
        Log.v(LOG_TAG, "Checking Subnet " + subnet);

        for (int i = 1; i < 255; i++) {
            final String host = subnet + "." + i;

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (InetAddress.getByName(host).isReachable(TIMEOUT)) {
                            Log.i(LOG_TAG, host + " is possible reachable");
                            if (checkHostWithPort(host, 8080) && checkHostWithPort(host, 80)) {
                                deviceList.add(host);
                                Log.i(LOG_TAG, host + " is reachable");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Could not check host " + host, e);
                    }
                }
            };
            es.execute(r);
        }

    }

    public static boolean checkHostWithPort(String host, int port) {
        Socket socket = new Socket();
        try {
            InetSocketAddress socketAddr = new InetSocketAddress(host, port);
            socket.connect(socketAddr, TIMEOUT);

            return true;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not check host" + host + " with port " + port, e);
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Could not close socket", e);
            }
        }
        return false;
    }



}
