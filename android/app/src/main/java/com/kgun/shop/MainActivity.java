package com.kgun.shop;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(isOnline()) new GetDataSet(this).execute();
        else showInternetDialog(this);
    }

    void init() {
        try {
            Spinner MoneyType = findViewById(R.id.Spinner_MoneyType);
            EditText Product = findViewById(R.id.ExitText_ProductPrice);
            EditText FixPrice = findViewById(R.id.EditText_FixPrice);
            EditText Delivery = findViewById(R.id.EditText_Delivery);
            EditText Amount = findViewById(R.id.EditText_Amount);
            Button Calculate = findViewById(R.id.Button_Calculate);
            CheckBox IncludeFee = findViewById(R.id.Checkbox_IncludeFee);
            TextView MoneyDetail = findViewById(R.id.MoneyDetail);
            TextView Result1 = findViewById(R.id.TextView_Result_1);
            TextView Result2 = findViewById(R.id.TextView_Result_2);
            String Money_Date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.KOREAN).format(new Date(Global.Money_Data.getLong("update")));
            Pattern pattern = Pattern.compile("[0-9]*\\.?[0-9]*");
            InputFilter[] NumberAndDot = new InputFilter[] {(charSequence, i, i1, spanned, i2, i3) -> charSequence.equals("") || pattern.matcher(charSequence).matches() ? charSequence : ""};

            Product.setFilters(NumberAndDot);
            FixPrice.setFilters(NumberAndDot);
            Delivery.setFilters(NumberAndDot);

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Global.Money_Type);
            MoneyType.setAdapter(arrayAdapter);
            MoneyType.setSelection(0);
            MoneyDetail.setText(MessageFormat.format("환율 : {0} KRW/USD ({1} 기준)", Global.Money_Data.getJSONArray("USDKRW").get(0), Money_Date));
            MoneyType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    try {
                        String mCode = Global.Money_Type.get(i).split(" ")[0];
                        MoneyDetail.setText(MessageFormat.format("환율 : {0} KRW/{1} ({2} 기준)", Global.Money_Data.getJSONArray(mCode + "KRW").get(0), mCode, Money_Date));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) { }
            });

            Intent intent = getIntent();
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                    intent.getAction().equals(Intent.ACTION_PROCESS_TEXT) &&
                    intent.hasExtra(Intent.EXTRA_PROCESS_TEXT)) {
                String textToProcess = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT).toString();
                String Processed = textToProcess.replaceAll("[^[[0-9]*.?[0-9]]]","");
                Product.setText(Processed);

                String textToProcessLower = textToProcess.toLowerCase();
                if(textToProcessLower.contains("hk$") || textToProcessLower.contains("hong kong dollar") || textToProcessLower.contains("hkd")) {
                    MoneyType.setSelection(1);
                } else if(textToProcessLower.contains("nt$") || textToProcessLower.contains("taiwan dollar") || textToProcessLower.contains("twd")) {
                    MoneyType.setSelection(2);
                } else if(textToProcessLower.contains("$") || textToProcessLower.contains("dollar") || textToProcessLower.contains("usd")) {
                    MoneyType.setSelection(0);
                } else if(textToProcessLower.contains("€") || textToProcessLower.contains("euro") || textToProcessLower.contains("eur")) {
                    MoneyType.setSelection(3);
                } else if(textToProcessLower.contains("¥") || textToProcessLower.contains("円") || textToProcessLower.contains("圓") || textToProcessLower.contains("yen") || textToProcessLower.contains("jpy")) {
                    MoneyType.setSelection(4);
                } else if(textToProcessLower.contains("元") || textToProcessLower.contains("renminbi") || textToProcessLower.contains("yuan") || textToProcessLower.contains("yuán") || textToProcessLower.contains("cny")) {
                    MoneyType.setSelection(5);
                }
            }
            
            Calculate.setOnClickListener((v) -> {
                String[] EditText = {"","","",""};
                EditText[0] = Product.getText().toString();
                EditText[1] = FixPrice.getText().toString();
                EditText[2] = Delivery.getText().toString();
                EditText[3] = Amount.getText().toString();
                boolean isBlankValue = false;

                for(String s : EditText) {
                    if(s.equals("")) {
                        isBlankValue = true;
                        break;
                    }
                }

                if(!isBlankValue) {
                    try {
                        DecimalFormat format = new DecimalFormat("###,###");
                        String mCode =  MoneyType.getSelectedItem().toString().split(" ")[0];
                        double total = Double.parseDouble(EditText[0])
                                * Integer.parseInt(EditText[3])
                                + Double.parseDouble(EditText[1])
                                + Double.parseDouble(EditText[2]);
                        String Content = "";
                        Content += "소계 : " + format.format(total) + " " + mCode + "\n";
                        Content += "검사비 : 55,000 KRW\n";

                        double ResultPrice;
                        double OverPrice;
                        if(mCode.equals("USD")) OverPrice = 150;
                        else OverPrice = Global.Money_Data.getJSONArray("USD" + mCode).getDouble(0) * 150;
                        if(Double.parseDouble(EditText[0]) >= OverPrice) {
                            ResultPrice = total * 1.2;
                        } else ResultPrice = total;
                        ResultPrice = ResultPrice * Global.Money_Data.getJSONArray(mCode + "KRW").getDouble(0) + 55000 + (IncludeFee.isChecked() ? 4000 : 0);
                        Result1.setText(Content);
                        Result2.setText(MessageFormat.format("총 계산 결과 : {0} KRW", format.format(ResultPrice)));
                        Toast.makeText(this,"계산 완료!",Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    String ErrorHint = getString(R.string.EditText_Hint);
                    if(EditText[0].equals("")) Product.setError(ErrorHint);
                    if(EditText[1].equals("")) FixPrice.setError(ErrorHint);
                    if(EditText[2].equals("")) Delivery.setError(ErrorHint);
                    if(EditText[3].equals("")) Amount.setError(ErrorHint);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        if (cm.getActiveNetworkInfo() != null) {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        }
        return false;
    }

    public void showInternetDialog(Activity context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setTitle("인터넷 연결 없음")
                .setMessage("인터넷 연결 확인후 재시도 바랍니다")
                .setCancelable(false)
                .setPositiveButton("확인",(d,w) -> context.finishAndRemoveTask())
                .show();
    }

    private class GetDataSet extends AsyncTask<Void,Void,String> {

        Activity context;
        boolean isConnected;

        protected GetDataSet(Activity context) {
            this.context = context;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(!isConnected) {
                showInternetDialog(context);
            } else {
                context.findViewById(R.id.Progress_Layout).setVisibility(View.GONE);
                init();
            }
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                isConnected = Global.netIsAvailable();
                Global.Money_Data = Global.readJsonFromUrl("https://earthquake.kr:23490/query/USDKRW,HKDKRW,TWDKRW,EURKRW,JPYKRW,CNYKRW,USDHKD,USDTWD,USDEUR,USDJPY,USDCNY");
            } catch (Exception e) {
                return e.toString();
            }
            return "";
        }
    }
}