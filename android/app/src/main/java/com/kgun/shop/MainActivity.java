package com.kgun.shop;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static JSONObject selectedCurrency;
    private static String TransactionMethod = "ttSellingPrice";
    private static String Money_Date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(isOnline()) new GetDataSet(this).execute();
        else showInternetDialog(this);
    }

    void init() {
        try {
            if(BuildConfig.DEBUG) Log.d("KGunCalcMoneyData","JSON :" + Global.Money_Data);
            MaterialAutoCompleteTextView MoneyType = findViewById(R.id.Spinner_MoneyType);
            TextInputEditText Product = findViewById(R.id.ExitText_ProductPrice);
            TextInputEditText FixPrice = findViewById(R.id.EditText_FixPrice);
            TextInputEditText Delivery = findViewById(R.id.EditText_Delivery);
            TextInputEditText Amount = findViewById(R.id.EditText_Amount);

            Button Calculate = findViewById(R.id.Button_Calculate);
            CheckBox IncludeFee = findViewById(R.id.Checkbox_IncludeFee);
            CheckBox UseSpread = findViewById(R.id.Checkbox_UseSpread);

            TextView MoneyDetail = findViewById(R.id.MoneyDetail);
            TextView Result1 = findViewById(R.id.TextView_Result_1);
            TextView Result2 = findViewById(R.id.TextView_Result_2);

            Pattern pattern = Pattern.compile("[0-9]*\\.?[0-9]*");
            InputFilter[] NumberAndDot = new InputFilter[] {(charSequence, i, i1, spanned, i2, i3) -> charSequence.equals("") || pattern.matcher(charSequence).matches() ? charSequence : ""};

            Product.setFilters(NumberAndDot);
            FixPrice.setFilters(NumberAndDot);
            Delivery.setFilters(NumberAndDot);

            UseSpread.setOnCheckedChangeListener((buttonView, isChecked) -> {
                TransactionMethod = isChecked ? "cashBuyingPrice" : "ttSellingPrice";
                try {
                    MoneyDetail.setText(MessageFormat.format("환율 : {0} KRW/{1} ({2} 기준)", selectedCurrency.getInt(TransactionMethod), MoneyType.getText().toString().split(" ")[0], Money_Date));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });

            setSelectedCurrency("KRWUSD");
            Money_Date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.KOREAN).format(new Date(selectedCurrency.getLong("timestamp")));

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Global.Money_Type);
            MoneyType.setAdapter(arrayAdapter);
            MoneyType.setText(arrayAdapter.getItem(0),false);
            MoneyDetail.setText(MessageFormat.format("환율 : {0} KRW/USD ({1} 기준)", selectedCurrency.getInt(TransactionMethod), Money_Date));
            MoneyType.setOnItemClickListener((parent, view, position, id) -> {
                try {
                    String mCode = Global.Money_Type.get(position).split(" ")[0];
                    setSelectedCurrency("KRW" + mCode);
                    Money_Date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.KOREAN).format(new Date(selectedCurrency.getLong("timestamp")));
                    MoneyDetail.setText(MessageFormat.format("환율 : {0} KRW/{1} ({2} 기준)", selectedCurrency.getInt(TransactionMethod), mCode, Money_Date));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            Intent intent = getIntent();
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                    intent.getAction().equals(Intent.ACTION_PROCESS_TEXT) &&
                    intent.hasExtra(Intent.EXTRA_PROCESS_TEXT)) {
                String textToProcess = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT).toString();
                String Processed = textToProcess.replaceAll("[^[0-9*.?0-9]]","");
                Product.setText(Processed);

                String textToProcessLower = textToProcess.toLowerCase();
                if(textToProcessLower.contains("hk$") || textToProcessLower.contains("hong kong dollar") || textToProcessLower.contains("hkd")) {
                    MoneyType.setText(arrayAdapter.getItem(1), false);
                } else if(textToProcessLower.contains("nt$") || textToProcessLower.contains("taiwan dollar") || textToProcessLower.contains("twd")) {
                    MoneyType.setText(arrayAdapter.getItem(2), false);
                } else if(textToProcessLower.contains("$") || textToProcessLower.contains("dollar") || textToProcessLower.contains("usd")) {
                    MoneyType.setText(arrayAdapter.getItem(0), false);
                } else if(textToProcessLower.contains("€") || textToProcessLower.contains("euro") || textToProcessLower.contains("eur")) {
                    MoneyType.setText(arrayAdapter.getItem(3), false);
                } else if(textToProcessLower.contains("¥") || textToProcessLower.contains("円") || textToProcessLower.contains("圓") || textToProcessLower.contains("yen") || textToProcessLower.contains("jpy")) {
                    MoneyType.setText(arrayAdapter.getItem(4), false);
                } else if(textToProcessLower.contains("元") || textToProcessLower.contains("renminbi") || textToProcessLower.contains("yuan") || textToProcessLower.contains("yuán") || textToProcessLower.contains("cny")) {
                    MoneyType.setText(arrayAdapter.getItem(5), false);
                }
            }
            
            Calculate.setOnClickListener((v) -> {
                String[] EditText = {"","","",""};
                EditText[0] = "" + Product.getText();
                EditText[1] = "" + FixPrice.getText();
                EditText[2] = "" + Delivery.getText();
                EditText[3] = "" + Amount.getText();
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
                        String mCode =  MoneyType.getText().toString().split(" ")[0];
                        double total = Double.parseDouble(EditText[0])
                                * Integer.parseInt(EditText[3])
                                + Double.parseDouble(EditText[1])
                                + Double.parseDouble(EditText[2]);
                        String Content = "";
                        Content += "소계 : " + format.format(total) + " " + mCode + "\n";
                        Content += "검사비 : 55,000 KRW\n";

                        double ResultPrice;
                        double OverPrice = 150;
                        if(!mCode.equals("USD")) {
                            JSONObject obj = getSelectedCurrency(mCode + "USD");
                            if(obj != null) OverPrice = obj.getDouble(TransactionMethod) * 150;
                        }

                        if(Double.parseDouble(EditText[0]) >= OverPrice) {
                            ResultPrice = total * 1.2;
                        } else ResultPrice = total;
                        ResultPrice = ResultPrice * selectedCurrency.getDouble(TransactionMethod) + 55000 + (IncludeFee.isChecked() ? 4000 : 0);
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

    private static void setSelectedCurrency(String CCode) throws JSONException {
        for(int i = 0;i <= Global.Money_Data.length();i++) {
            JSONObject obj = Global.Money_Data.getJSONObject(i);
            if(obj.getString("code").equals("FRX." + CCode)) {
                selectedCurrency = obj;
                break;
            }
        }
    }

    private static JSONObject getSelectedCurrency(String CCode) throws JSONException {
        for(int i = 0;i <= Global.Money_Data.length();i++) {
            JSONObject obj = Global.Money_Data.getJSONObject(i);
            if(obj.getString("code").equals("FRX." + CCode)) {
                return obj;
            }
        }
        return null;
    }

    @SuppressLint("MissingPermission")
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
                Global.Money_Data = Global.readJsonFromUrl("https://quotation-api-cdn.dunamu.com/v1/forex/recent?codes=FRX.KRWUSD,FRX.KRWHKD,FRX.KRWTWD,FRX.KRWEUR,FRX.KRWJPY,FRX.KRWCNY,FRX.TWDUSD,FRX.EURUSD,FRX.JPYUSD,FRX.CNYUSD");
            } catch (Exception e) {
                return e.toString();
            }
            return "";
        }
    }
}