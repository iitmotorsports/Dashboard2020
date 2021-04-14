package sae.iit.saedashboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class CANDialog {

    private final AlertDialog dialog;
    private final Timer CANMsgSend = new Timer();

    CANDialog(Activity activity, TeensyStream stream){
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(activity);
        View mView = activity.getLayoutInflater().inflate(R.layout.canmsg_dialog_layout, null);

        Button Send = mView.findViewById(R.id.btnSend);
        Button Cancel = mView.findViewById(R.id.btnCancel);
        Button Clear = mView.findViewById(R.id.btnClear);
        Button Rand = mView.findViewById(R.id.btnRnd);
        ToggleButton Cont = mView.findViewById(R.id.btnCont);

        EditText address = mView.findViewById(R.id.editTextAddress);
        EditText byte0 = mView.findViewById(R.id.editTextByte0);
        EditText byte1 = mView.findViewById(R.id.editTextByte1);
        EditText byte2 = mView.findViewById(R.id.editTextByte2);
        EditText byte3 = mView.findViewById(R.id.editTextByte3);
        EditText byte4 = mView.findViewById(R.id.editTextByte4);
        EditText byte5 = mView.findViewById(R.id.editTextByte5);
        EditText byte6 = mView.findViewById(R.id.editTextByte6);
        EditText byte7 = mView.findViewById(R.id.editTextByte7);

        mBuilder.setView(mView);
        dialog = mBuilder.create();

        Runnable sendData = () -> {
            String addStr = "00000000".concat(address.getText().toString());
            addStr = addStr.substring(addStr.length() - 8);
            byte[] add = ByteSplit.hexToBytes(addStr);
            byte[] bytes = ByteSplit.joinArray(ByteSplit.hexToBytes(byte0.getText().toString()),
                    ByteSplit.hexToBytes(byte1.getText().toString()),
                    ByteSplit.hexToBytes(byte2.getText().toString()),
                    ByteSplit.hexToBytes(byte3.getText().toString()),
                    ByteSplit.hexToBytes(byte4.getText().toString()),
                    ByteSplit.hexToBytes(byte5.getText().toString()),
                    ByteSplit.hexToBytes(byte6.getText().toString()),
                    ByteSplit.hexToBytes(byte7.getText().toString()));

            if (bytes.length != 8 || add.length == 0 || add.length > 4) {
                Toaster.showToast("Message Not Sent", Toaster.STATUS.WARNING);
                return;
            }
            Toaster.showToast((ByteSplit.bytesToHex(add) + " : " + ByteSplit.bytesToHex(bytes).replaceAll("(.{2})", "$1 ")), Toaster.STATUS.INFO);
            stream.write(TeensyStream.COMMAND.SEND_CANBUS_MESSAGE);
            stream.write(add);
            stream.write(bytes);
        };

        Send.setOnClickListener(view -> sendData.run());

        Clear.setOnClickListener(view -> {
            address.setText("");
            byte0.setText("");
            byte1.setText("");
            byte2.setText("");
            byte3.setText("");
            byte4.setText("");
            byte5.setText("");
            byte6.setText("");
            byte7.setText("");
        });
        Random rnd = new Random();
        Rand.setOnClickListener(view -> {
            byte[] add = new byte[4];
            byte[] bytes = new byte[8];
            rnd.nextBytes(add);
            rnd.nextBytes(bytes);
            address.setText(ByteSplit.bytesToHex(add));
            byte0.setText(ByteSplit.bytesToHex(new byte[]{bytes[0]}));
            byte1.setText(ByteSplit.bytesToHex(new byte[]{bytes[1]}));
            byte2.setText(ByteSplit.bytesToHex(new byte[]{bytes[2]}));
            byte3.setText(ByteSplit.bytesToHex(new byte[]{bytes[3]}));
            byte4.setText(ByteSplit.bytesToHex(new byte[]{bytes[4]}));
            byte5.setText(ByteSplit.bytesToHex(new byte[]{bytes[5]}));
            byte6.setText(ByteSplit.bytesToHex(new byte[]{bytes[6]}));
            byte7.setText(ByteSplit.bytesToHex(new byte[]{bytes[7]}));
        });
        Cancel.setOnClickListener(view -> dialog.dismiss());

        final TimerTask[] CAN_Task = new TimerTask[1];

        Cont.setOnClickListener(view -> {
            if (((ToggleButton) view).isChecked()) {
                CAN_Task[0] = new TimerTask() {
                    @Override
                    public void run() {
                        sendData.run();
                    }
                };
                CANMsgSend.schedule(CAN_Task[0], 0, 1100); // Not exactly a second just in case
                Toaster.showToast("Sending message every second", Toaster.STATUS.INFO);
            } else {
                CAN_Task[0].cancel();
                CANMsgSend.purge();
                Toaster.showToast("Stopping CAN Messages", Toaster.STATUS.INFO);
            }
        });

        mView.getViewTreeObserver().addOnGlobalLayoutListener(
                () -> dialog.getWindow().setLayout(Math.max(address.getWidth() * 2 + (byte0.getWidth() * 8), Cont.getWidth() * 5), dialog.getWindow().getAttributes().height)
        );
    }

    public void showDialog() {
        if (!dialog.isShowing())
            dialog.show();
    }
}
