package com.deskconn.nfcreaddata;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ReadAndWriteTag extends AppCompatActivity {

    public static final String ERROR_DETECTED = "No NFC tag detected";
    public static final String WRITE_SUCCESS = "Text written successfully!";
    public static final String WRITE_ERROR = "Error during writing, Try again!";
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter[] writingTagFilter;
    boolean writeMode;
    Tag myTag;
    Context context;
    TextView editText;
    TextView textView;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_and_write_tag);

        editText = (TextView) findViewById(R.id.edit_text);
        textView = findViewById(R.id.textview);
        button = findViewById(R.id.button);
        context = this;

        button.setOnClickListener(view -> {
            try {
                if (myTag == null) {
                    Toast.makeText(context, ERROR_DETECTED, Toast.LENGTH_SHORT).show();
                } else {
                    write(editText.getText().toString(), myTag);
                    Toast.makeText(context, WRITE_SUCCESS, Toast.LENGTH_SHORT).show();
                    editText.setText("");
                }
            } catch (Exception e) {
                System.out.println("here  " + e);
                Toast.makeText(context, WRITE_ERROR + e, Toast.LENGTH_SHORT).show();
            }
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        if (nfcAdapter == null) {
            Toast.makeText(context, "This device not support NFC. ", Toast.LENGTH_SHORT).show();
            finish();
        }

        readFromIntent(getIntent());

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writingTagFilter = new IntentFilter[]{tagDetected};
    }

    private void write(String text, Tag myTag) throws IOException, FormatException {
        NdefRecord[] records = {createRecords(text)};
        NdefMessage ndefMessage = new NdefMessage(records);

        Ndef ndef = Ndef.get(myTag);

        ndef.connect();

        ndef.writeNdefMessage(ndefMessage);

        ndef.close();

//        NdefFormatable formatable = NdefFormatable.get(myTag);
//        formatable.connect();
//        formatable.format(ndefMessage);
//        formatable.close();
    }

    private NdefRecord createRecords(String text) {

        String lang = "en"; //ex en
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes(StandardCharsets.US_ASCII);
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];

        payload[0] = (byte) langLength;

        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord ndefRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT,
                new byte[0], payload);

        return ndefRecord;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readFromIntent(intent);

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        writeModeOff();
    }

    private void writeModeOff() {
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        writeModeOn();
    }

    private void writeModeOn() {
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writingTagFilter, null);
    }

    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] message = null;
            if (rawMessages != null) {
                message = new NdefMessage[rawMessages.length];
                for (int i = 0; i < rawMessages.length; i++) {
                    message[i] = (NdefMessage) rawMessages[i];
                }
            }

            buildTagViews(message);
        }
    }

    private void buildTagViews(NdefMessage[] message) {
        if (message == null || message.length == 0) {
            return;
        }

        String text = "";

        byte[] payload = message[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 51;
        try {
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1,
                    textEncoding);
        } catch (Exception e) {
            System.out.println("Unsupported Encoding");
        }
        textView.setText("NFC content: " + text);
    }
}