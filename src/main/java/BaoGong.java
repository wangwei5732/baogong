import com.baidu.aip.ocr.AipOcr;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.json.Json;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/**
 * @Auther: wangwei
 * @Date: 2019-07-19 22:25
 * @Description:报工模拟填报
 */
public class BaoGong {
    //  登录页
    private static String logonAction = "http://sapmb01.hollysys.net:5280/OnDemand/logonAction.action";
    //  报工页面
    //  新建补录页面
    private static String qeruyworkTimeReportAction = "http://sapmb01.hollysys.net:5280/OnDemand/wrkmng/bcmng/qeruyworkTimeReportAction.action";
    private static String createReportActionReportAction = "http://sapmb01.hollysys.net:5280/OnDemand/wrkmng/bcmng/createReportAction.action?jobworkid=";
    // 报工填写页面
    private static String updWorkReportAction = "http://sapmb01.hollysys.net:5280/OnDemand/wrkmng/bcmng/updWorkReportAction.action?ser=";

    public static void main(String[] args) {
        BaoGongConfig baoGongConfig = BaoGong.loadConfig();
        //    角色
        RoleEnum role = RoleEnum.GCS;

        //配置webdriver
        System.setProperty("webdriver.chrome.driver", BaoGong.getChromeDriverUrl());
        WebDriver webDriver = new ChromeDriver();
        webDriver.manage().window().setSize(new Dimension(1024, 768));
        Scanner scanner = new Scanner(System.in);
        try {

            //1.进入网站并模拟登陆
            webDriver.get(logonAction);
            Alert alert = webDriver.switchTo().alert();
            //接受alert弹窗
            alert.accept();
            // 获取验证码
            String validateStr = BaoGong.getValidateCode(webDriver);

            //开始模拟登陆
            webDriver.findElement(By.name("txtEmpId")).sendKeys(baoGongConfig.getUsername());
            String changeRoleScript = "document.loginform.txtRole.value =\"" + role.getRoleVale() + "\"";
            ((ChromeDriver) webDriver).executeScript(changeRoleScript);
            webDriver.findElement(By.name("txtPwd")).sendKeys(baoGongConfig.getPassword());
            webDriver.findElement(By.name("txtRand")).sendKeys(validateStr);
            ((ChromeDriver) webDriver).findElementById("btSubmit").click();

            //2、报工页面
            webDriver.navigate().to(BaoGong.qeruyworkTimeReportAction);
            //已报工列表查询页
            String listPageUrl = "";
            //报工填写时需要用
            String workName = "";
            String workId = "";
            //循环遍历出当前时间的任务
            List<WebElement> elements = webDriver.findElements(By.className("pEven"));
            int nowDate = Integer.valueOf(DateFormatUtils.format(new Date(), "yyyyMMdd"));
            for (int i = elements.size() - 1; i >= 0; i--) {
                WebElement ele = elements.get(i);
                String startDateStr = ele.findElement(By.xpath("td[10]/div")).getAttribute("title");
                String endDateStr = ele.findElement(By.xpath("td[11]/div")).getAttribute("title");
                int startDate = Integer.valueOf(startDateStr.replace("-", ""));
                int endDate = Integer.valueOf(endDateStr.replace("-", ""));
                if (nowDate <= endDate && nowDate >= startDate) {
                    WebElement firstRadioBox = ele.findElement(By.id("radBox"));
                    //内容：singleRadBox(this,'500024284202101-184255','184255','R-D791803MS5000242842','1');
                    String[] firstRadioParam = firstRadioBox.getAttribute("onclick").split(",");
                    workName = ele.findElement(By.xpath("td[8]/div")).getAttribute("title");
                    workId = firstRadioParam[1];
                    String empCode = firstRadioParam[2];
                    String txtCustId = firstRadioParam[3];
                    String checkstuta = firstRadioParam[4].split("\\)")[0];
                    //组装赋值script
                    String workListPageUrlScript = "return 'http://sapmb01.hollysys.net:5280/OnDemand/wrkmng/bcmng/updWorkReportAction.action?ser=' + encodeURI(encodeURI(getDate()))+'&jobworkid='+encodeURI(encodeURI(" + workId
                            + "))+'&paEmpCode='+" + empCode + "+'&txtCustId='+" + txtCustId + "+'&checkstuta='+" + checkstuta;
                    listPageUrl = ((ChromeDriver) webDriver).executeScript(workListPageUrlScript).toString();
                }
            }
            //3.去工时查询页（填报工时必须去这个页面一次，否则报错）
            webDriver.navigate().to(listPageUrl);
            //4进入新建补录页面
            webDriver.navigate().to(createReportActionReportAction + workId.replace("'", ""));
//            String txtQPlanEdt = LocalDate.now().toString();
            String txtQPlanEdt = "" + nowDate;
            String txtWorknumber = "1";
            String txtContextDescription = workName;


            String changeDateScript = "document.getElementById(\"txtQPlanEdt\").value='"+baoGongConfig.getDate()+"'";
            ((ChromeDriver) webDriver).executeScript(changeDateScript);
            ((ChromeDriver) webDriver).findElementById("txtWorknumber").sendKeys(txtWorknumber);
            ((ChromeDriver) webDriver).findElementById("txtContextDescription").sendKeys(baoGongConfig.getDescription());
            WebElement commitBtn = ((ChromeDriver) webDriver).findElement(By.xpath("//*[@id=\"createReportAction\"]/table[1]/tbody/tr[2]/td[2]/span[1]"));
            commitBtn.click();

            Alert alert2 = webDriver.switchTo().alert();
            //接受alert弹窗
            alert2.accept();

            webDriver.navigate().to(listPageUrl);
            System.out.println("输入1，并回车结束：");
            scanner.next();
            System.out.println("结束");

        } catch (UnhandledAlertException e) {
            if (e.getMessage().indexOf("验证码输入有误,请重新输入") > 0) {
                System.out.println("验证码识别错误，请重新运行代码！");
            }
            if ("新增补录成功".equals(e.getAlertText())) {
                System.out.println("新增补录成功!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            webDriver.quit();
            scanner.close();
        }

    }

    /**
     * @return java.lang.String
     * @Author wangwei
     * @Description //TODO 获取图片验证码
     * @Date 18:01 2019-07-21
     * @Param []
     **/
    public static String getValidateStr() {
        String validateStr = "";
        try {
            //百度图像识别获取验证码
            String APP_ID = "16853191";
            String API_KEY = "zHZ6eEbl2gnisDe8g34wBSd9";
            String SECRET_KEY = "mERYGVPxnTb7Tl5yze3qksCMhF23DK44";
            // 初始化一个AipOcr
            AipOcr client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);
            // 调用接口
            String path = "validateImg/validateImg.png";
            JSONObject res = client.basicGeneral(path, new HashMap<String, String>());
            validateStr = res.getJSONArray("words_result").getJSONObject(0).getString("words");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return validateStr;
    }

    /**
     * 根据系统类型获取chromedriver
     *
     * @return chromedriver Url
     */
    private static String getChromeDriverUrl() {
        Properties prop = System.getProperties();
        String osName = prop.getProperty("os.name");
        String url = BaoGong.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String path = url.substring(0, url.lastIndexOf("/"));
        if (osName.startsWith("Mac OS")) {
            // 苹果
            return path + "/chromedriver";
        } else {
            // windows
            return path + "/chromedriver.exe";
        }
    }

    /**
     * 截取验证码，使用百度地图api进行识别，返回验证码
     *
     * @param webDriver
     * @return 验证码
     * @throws Exception
     */
    private static String getValidateCode(WebDriver webDriver) throws Exception {
        //获取验证码element
        WebElement validateCode = ((ChromeDriver) webDriver).findElement(By.xpath("//*[@id=\"mainFrame\"]/table[1]/tbody/tr[2]/td[2]/table/tbody/tr[5]/td[2]/span/img"));
        //截取整个网站图片
        File screenshot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE);
        BufferedImage fullImage = ImageIO.read(screenshot);
        Point point = validateCode.getLocation();

        int width = validateCode.getSize().getWidth();
        int height = validateCode.getSize().getHeight();
        int imgX = point.getX();
        int imgY = point.getY();

        // mark
        Properties prop = System.getProperties();
        String osName = prop.getProperty("os.name");
        if (osName.startsWith("Mac OS")) {
            // Mac
            // 获取网站页面尺寸，并计算尺寸比例（网站/图片）
            Dimension dimension = webDriver.manage().window().getSize();
            double scaleW = (double) dimension.getWidth() / fullImage.getWidth();
            double scaleH = scaleW;
            width = (int) (validateCode.getSize().getWidth() / scaleW);
            height = (int) (validateCode.getSize().getHeight() / scaleH);
            imgX = (int) (point.getX() / scaleW);
            imgY = (int) (point.getY() / scaleH);
        }
        BufferedImage validateImg = fullImage.getSubimage(imgX, imgY, width, height);

        File file = new File("validateImg", "validateImg.png");
        if (!file.exists()) {
            file.getParentFile().mkdir();
        }
        ImageIO.write(validateImg, "png", file);
        //获取验证码，添加容错，调用两次
        String validateStr = BaoGong.getValidateStr();
        if (StringUtils.isEmpty(validateStr) || validateStr.length() != 4) {
            validateStr = BaoGong.getValidateStr();
        }
        return validateStr;
    }

    /**
     * 加载参数
     *
     * @return
     */
    private static BaoGongConfig loadConfig() {
        String url = BaoGong.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String path = url.substring(0, url.lastIndexOf("/"));
        String baogongStr = BaoGong.readTxt(path + "/baogong.txt");
        BaoGongConfig baoGongConfig = new Gson().fromJson(baogongStr, BaoGongConfig.class);
        return baoGongConfig;
    }

    /**
     * 读取文本
     *
     * @param filePath
     * @return
     */
    public static String readTxt(String filePath) {
        String txt = "";
        try {
            File file = new File(filePath);
            if (file.isFile() && file.exists()) {
                String lineTxt = "";
                InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                while ((lineTxt = br.readLine()) != null) {
                    System.out.println(lineTxt);
                    txt += lineTxt;
                }
                br.close();
            } else {
                System.out.println("文件不存在!");
            }
        } catch (Exception e) {
            System.out.println("文件读取错误!");
        } finally {
            return txt;
        }

    }
}
