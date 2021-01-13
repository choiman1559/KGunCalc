import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class GunShopCalculator {
    public static void main(String[] args) {
        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
        terminalFactory.setInitialTerminalSize(new TerminalSize(90,27));
        terminalFactory.setTerminalEmulatorTitle("직구 가격 계산기");
        TerminalScreen terminal;

        try {
            terminal = terminalFactory.createScreen();

            Screen screen = terminal;
            screen.startScreen();
            screen.doResizeIfNecessary();

            final WindowBasedTextGUI textGUI = new MultiWindowTextGUI(screen);
            final Window window = new BasicWindow(" 직구 가격 계산기 v.1.1 ");

            Panel contentPanel = new Panel(new GridLayout(2));
            GridLayout gridLayout = (GridLayout) contentPanel.getLayoutManager();
            gridLayout.setHorizontalSpacing(3);

            if(!netIsAvailable()) {
                contentPanel.addComponent(new Label("인터넷 연결 없음!\n인터넷 연결 확인 후 재시도 바랍니다."));
                window.setComponent(contentPanel);
                textGUI.addWindowAndWait(window);
            }
            JSONObject ChangeMoneyInfo = readJsonFromUrl("https://earthquake.kr:23490/");

            Label title = new Label("직구 가격을 계산 합니다. 키보드를 이용 하여 항목을 선택/수정 합니다. \nMade by. Choiman1559 / This Software is Free-software under LGPL-3.0 license.");
            title.setLayoutData(GridLayout.createLayoutData(
                    GridLayout.Alignment.BEGINNING, GridLayout.Alignment.BEGINNING, true, false, 2, 1));
            contentPanel.addComponent(title);
            contentPanel.addComponent(new EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2)));

            contentPanel.addComponent(new Label("거래 통화"));
            List<String> countries = new ArrayList<>();
            countries.add("USD (미국 달러)");
            countries.add("HKD (홍콩 달러)");
            countries.add("TWD (대만 달러)");
            countries.add("EUR (유럽 유로)");
            countries.add("JPY (일본 엔화)");
            ComboBox<String> Money_Type = new ComboBox<>(countries);
            Money_Type.setReadOnly(true);
            Money_Type.setPreferredSize(new TerminalSize(20, 1));
            contentPanel.addComponent(Money_Type);
            contentPanel.addComponent(new EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2)));

            TextBox Price_Product = new TextBox()
                    .setPreferredSize(new TerminalSize(20,1))
                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.CENTER))
                    .setValidationPattern(Pattern.compile("[0-9]*\\.?[0-9]*"));
            contentPanel.addComponent(new Label("제품 가격"));
            contentPanel.addComponent(Price_Product);
            contentPanel.addComponent(new EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2)));

            TextBox Price_Fixes = new TextBox()
                    .setPreferredSize(new TerminalSize(20,1))
                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.CENTER))
                    .setValidationPattern(Pattern.compile("[0-9]*\\.?[0-9]*"));
            contentPanel.addComponent(new Label("공임비"));
            contentPanel.addComponent(Price_Fixes);
            contentPanel.addComponent(new EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2)));

            TextBox Price_Delivery = new TextBox()
                    .setPreferredSize(new TerminalSize(20,1))
                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.CENTER))
                    .setValidationPattern(Pattern.compile("[0-9]*\\.?[0-9]*"));
            contentPanel.addComponent(new Label("배송료"));
            contentPanel.addComponent(Price_Delivery);
            contentPanel.addComponent(new EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2)));

            TextBox Amount_Guns = new TextBox()
                    .setPreferredSize(new TerminalSize(20,1))
                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.CENTER))
                    .setValidationPattern(Pattern.compile("[0-9]*"));
            contentPanel.addComponent(new Label("물건 수량"));
            contentPanel.addComponent(Amount_Guns);
            contentPanel.addComponent(new EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2)));

            CheckBox Add_Fees = new CheckBox()
                    .setPreferredSize(new TerminalSize(20,1))
                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.CENTER));
            contentPanel.addComponent(new Label("통관대행수수료 적용"));
            contentPanel.addComponent(Add_Fees);
            contentPanel.addComponent(new EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2)));

            Button Calculate = new Button(" 계산 ", () -> {
                if(Price_Product.getText().equals("") || Price_Fixes.getText().equals("") || Price_Delivery.getText().equals("") || Amount_Guns.getText().equals("")) {
                    MessageDialog.showMessageDialog(textGUI, " 오류! ", "값을 모두 입력하십시오!", MessageDialogButton.OK);
                } else {
                    try {
                        DecimalFormat format = new DecimalFormat("###,###.##");
                        String mCode =  Money_Type.getText().split(" ")[0];
                        double total = Double.parseDouble(Price_Product.getText())
                                * Integer.parseInt(Amount_Guns.getText())
                                + Double.parseDouble(Price_Delivery.getText())
                                + Double.parseDouble(Price_Fixes.getText());
                        String Content = "";
                        Content += "소계 : " + format.format(total) + " " + mCode + "\n";
                        Content += "검사비 : 55,000 KRW\n\n";
                        Content += "총 계산 결과 : ";

                        double ResultPrice;
                        double OverPrice;
                        if(mCode.equals("USD")) OverPrice = 150;
                        else OverPrice = ChangeMoneyInfo.getJSONArray("USD" + mCode).getDouble(0) * 150;
                        if(Double.parseDouble(Price_Product.getText()) >= OverPrice) {
                            ResultPrice = (int)(total * 1.2);
                        } else ResultPrice = total;
                        ResultPrice = ResultPrice * ChangeMoneyInfo.getJSONArray(mCode + "KRW").getDouble(0) + 55000 + (Add_Fees.isChecked() ? 4000 : 0);
                        Content += format.format(ResultPrice) + " KRW";

                        MessageDialog.showMessageDialog(textGUI, "계산 결과", Content, MessageDialogButton.OK);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.CENTER));
            contentPanel.addComponent(new Label("결과 보기"));
            contentPanel.addComponent(Calculate);

            String Money_Date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date(ChangeMoneyInfo.getLong("update")));
            Label Change_Money_Info = new Label("환율 : " + ChangeMoneyInfo.getJSONArray("USDKRW").get(0) + " KRW/USD (" + Money_Date + " 기준)");
            contentPanel.addComponent(new EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2)));
            contentPanel.addComponent(new Separator(Direction.HORIZONTAL).setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2)));
            contentPanel.addComponent(Change_Money_Info);
            contentPanel.addComponent(new Button(" 종료 ", window::close).setLayoutData(GridLayout.createHorizontallyEndAlignedLayoutData(2)));

            Money_Type.addListener((selectedIndex, previousSelection, changedByUserInteraction) -> {
                String mCode = countries.get(selectedIndex).split(" ")[0];
                Change_Money_Info.setText("환율 : " + ChangeMoneyInfo.getJSONArray(mCode + "KRW").get(0) + " KRW/" + mCode + " (" + Money_Date + " 기준)");
            });

            window.setComponent(contentPanel);
            textGUI.addWindowAndWait(window);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    private static boolean netIsAvailable() {
        try {
            final URL url = new URL("http://www.google.com");
            final URLConnection conn = url.openConnection();
            conn.connect();
            conn.getInputStream().close();
            return true;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            return false;
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }
}
