package sae.iit.saedashboard;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class JSONQR {
    private final Activity MainActivity;
    private IntentIntegrator scanIntegrator;

    private static SortedMap<Byte, byte[]> qrByteMap = new TreeMap<>();
    private static byte expected = -1;


    public JSONQR(Activity MainActivity) {
        this.MainActivity = MainActivity;
        scanIntegrator = new IntentIntegrator(MainActivity);
        scanIntegrator.setPrompt("Scan qrJSON.gif until max");
        scanIntegrator.setBeepEnabled(false);
    }

    public void initiate() {
        scanIntegrator.initiateScan(Collections.singletonList(IntentIntegrator.QR_CODE));
    }

    static byte[] joinArray(Collection<byte[]> arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }

        final byte[] result = new byte[length];

        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }

        return result;
    }

    public String getData() {
        List<byte[]> data = new ArrayList<>();
        for (Map.Entry<Byte, byte[]> e : qrByteMap.entrySet()) {
            data.add(e.getValue());
        }
        byte[] rawBytes = joinArray(data);
        int size = (int) ByteSplit.getUnsignedInt(rawBytes);
        byte[] rawData = Arrays.copyOfRange(rawBytes, 4, rawBytes.length);
        try {
            Inflater decompressor = new Inflater();
            decompressor.setInput(rawData);
            byte[] result = new byte[size + 1];
            int resultLength = decompressor.inflate(result);
            decompressor.end();
            return new String(result, 0, resultLength, StandardCharsets.UTF_8);
        } catch (DataFormatException e) {
            e.printStackTrace();
            Toaster.showToast("Failed to decode QR data", Toaster.STATUS.ERROR);
        }
        return "";
    }

    public void clear() {
        qrByteMap.clear();
        expected = -1;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode != IntentIntegrator.REQUEST_CODE)
            return;
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, resultData);
        byte[] content = scanningResult.getContents().getBytes(StandardCharsets.ISO_8859_1);
        byte[] bytes = android.util.Base64.decode(content, android.util.Base64.DEFAULT);
        byte[] raw_bytes = Arrays.copyOfRange(bytes, 2, bytes.length);

        expected = bytes[0];
        qrByteMap.put(bytes[1], raw_bytes);

        byte last = -1;
        Set<Byte> keys = qrByteMap.keySet();
        for (byte b : keys) {
            if (last == -1) {
                if (b != 0) { // Numbering must start at 0
                    break;
                }
            } else if (b - last != 1) {
                break;
            }
            last = b;
        }

        StringBuilder dataMap = new StringBuilder();
        dataMap.append("Data:");
        for (byte i = 0; i <= expected; i++) {
            if (bytes[1] == i)
                dataMap.append('█');
            else if (keys.contains(i))
                dataMap.append('■');
            else
                dataMap.append(" ").append(i).append(" ");
        }
        Toaster.showToast(dataMap.toString(), false, true, Toaster.STATUS.INFO);
        if (last != expected) {
            scanIntegrator.initiateScan(Collections.singletonList(IntentIntegrator.QR_CODE));
        } else {
            Toaster.showToast("Done with QR", false, true, Toaster.STATUS.SUCCESS);
            Log.i("JSONQR", getData());
        }
    }
}
