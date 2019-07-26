import com.baidu.aip.ocr.AipOcr;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

/**
 * @Auther: wangwei
 * @Date: 2019-07-19 22:25
 * @Description:报工模拟填报
 */
public class BaoGong {
    //  报工页面
    private static String qeruyworkTimeReportAction="http://sapmb01.hollysys.net:5280/OnDemand/wrkmng/bcmng/qeruyworkTimeReportAction.action";
    //  新建补录页面
    private static String createReportActionReportAction="http://sapmb01.hollysys.net:5280/OnDemand/wrkmng/bcmng/createReportAction.action?jobworkid=";

    public static void main(String[] args) {
        //    账号
        String userName="184255";
        //    密码
        String password="";
        //    角色
        RoleEnum role=RoleEnum.GCS;

        //配置webdriver
        System.setProperty("webdriver.chrome.driver", BaoGong.getChromeDriverUrl());
        WebDriver webDriver = new ChromeDriver();
        Scanner scanner  = new Scanner(System.in);
        try {

            //1.进入网站并模拟登陆
            webDriver.get("http://sapmb01.hollysys.net:5280/OnDemand/logonAction.action");
            Alert alert = webDriver.switchTo().alert();
            //接受alert弹窗
            alert.accept();

            //获取验证码element
            WebElement validateCode = ((ChromeDriver) webDriver).findElement(By.xpath("//*[@id=\"mainFrame\"]/table[1]/tbody/tr[2]/td[2]/table/tbody/tr[5]/td[2]/span/img"));
            //截取整个网站图片
            File screenshot = ((TakesScreenshot)webDriver).getScreenshotAs(OutputType.FILE);
            BufferedImage fullImage = ImageIO.read(screenshot);

            // 获取网站页面尺寸，并计算尺寸比例（网站/图片）
            Dimension dimension = webDriver.manage().window().getSize();
            double scaleW = (double) dimension.getWidth()/fullImage.getWidth();
            double scaleH = (double) dimension.getHeight()/fullImage.getHeight();

            Point point = validateCode.getLocation();
            int width = (int)(validateCode.getSize().getWidth()/scaleW) ;
            int height = (int) (validateCode.getSize().getHeight()/scaleH);
            int imgX =(int)(point.getX()/scaleW);
            int imgY =(int)(point.getY()/scaleH);

            //mark：不知道为什么高度有误差，直接多取了300，后期修改
            BufferedImage validateImg = fullImage.getSubimage(imgX,imgY,width,height+200);

            File file = new File("validateImg","validateImg.png");
            if (!file.exists()){
                file.getParentFile().mkdir();
            }
            ImageIO.write(validateImg,"png",file);
            //获取验证码，添加容错，调用两次
            String validateStr = BaoGong.getValidateStr();
            if(StringUtils.isEmpty(validateStr)||validateStr.length()!=4){
                validateStr = BaoGong.getValidateStr();
            }
            while (StringUtils.isEmpty(userName)||StringUtils.isEmpty(password)){
                //用户名密码输入
                if (StringUtils.isEmpty(userName)){
                    System.out.println("请输入用户名:");
                    userName=scanner.next();
                }else{
                    System.out.println("请输入密码:");
                    password=scanner.next();
                }
            }
            //开始模拟登陆
            webDriver.findElement(By.name("txtEmpId")).sendKeys(userName);
            String changeRoleScript = "document.loginform.txtRole.value =\""+role.getRoleVale()+"\"";
            ((ChromeDriver) webDriver).executeScript(changeRoleScript);
            webDriver.findElement(By.name("txtPwd")).sendKeys(password);
            webDriver.findElement(By.name("txtRand")).sendKeys(validateStr);
            ((ChromeDriver) webDriver).findElementById("btSubmit").click();

            //2、报工页面
            webDriver.navigate().to(BaoGong.qeruyworkTimeReportAction);
            //已报工列表查询页
            String listPageUrl = "";
            //报工填写时需要用
            String workName ="";
            String workId = "";
            //循环遍历出当前时间的任务
            List<WebElement> elements = webDriver.findElements(By.className("pEven"));
            int nowDate = Integer.valueOf(DateFormatUtils.format(new Date(),"yyyyMMdd"));
            for (int i= elements.size()-1 ;i>=0 ;i-- ) {
                WebElement ele = elements.get(i);
                String startDateStr = ele.findElement(By.xpath("td[10]/div")).getAttribute("title");
                String endDateStr = ele.findElement(By.xpath("td[11]/div")).getAttribute("title");
                int startDate =Integer.valueOf(startDateStr.replace("-",""));
                int endDate =Integer.valueOf(endDateStr.replace("-",""));
                if(nowDate<=endDate && nowDate>=startDate){
                    WebElement firstRadioBox = ele.findElement(By.id("radBox"));
                    //内容：singleRadBox(this,'500024284202101-184255','184255','R-D791803MS5000242842','1');
                    String[] firstRadioParam = firstRadioBox.getAttribute("onclick").split(",");
                    workName = ele.findElement(By.xpath("td[8]/div")).getAttribute("title");
                    workId = firstRadioParam[1];
                    String empCode = firstRadioParam[2];
                    String txtCustId = firstRadioParam[3];
                    String checkstuta = firstRadioParam[4].split("\\)")[0];
                    //组装赋值script
                    String workListPageUrlScript = "return 'http://sapmb01.hollysys.net:5280/OnDemand/wrkmng/bcmng/updWorkReportAction.action?ser=' + encodeURI(encodeURI(getDate()))+'&jobworkid='+encodeURI(encodeURI("+workId
                            +"))+'&paEmpCode='+"+empCode+"+'&txtCustId='+"+txtCustId+"+'&checkstuta='+"+checkstuta;
                    listPageUrl = ((ChromeDriver) webDriver).executeScript(workListPageUrlScript).toString();
                }
            }
            //3.去工时查询页（填报工时必须去这个页面一次，否则报错）
            webDriver.navigate().to(listPageUrl);
            System.out.println("是否自动填写工时：（1：是 2 否）");
            if(!"1".equals(scanner.next())){
                throw new Exception("中断");
            }
            //4进入新建补录页面
            webDriver.navigate().to(createReportActionReportAction+workId.replace("'",""));
//            String txtQPlanEdt = LocalDate.now().toString();
            String txtQPlanEdt = "";
            String txtWorknumber = "1";
            String txtContextDescription = workName;


            ((ChromeDriver) webDriver).findElementById("txtQPlanEdt").sendKeys(txtQPlanEdt);
            ((ChromeDriver) webDriver).findElementById("txtWorknumber").sendKeys(txtWorknumber);
            ((ChromeDriver) webDriver).findElementById("txtContextDescription").sendKeys(txtContextDescription);
            WebElement commitBtn = ((ChromeDriver) webDriver).findElement(By.xpath("//*[@id=\"createReportAction\"]/table[1]/tbody/tr[2]/td[2]/span[1]"));
            commitBtn.click();

            Alert alert2 = webDriver.switchTo().alert();
            //接受alert弹窗
            alert2.accept();

            webDriver.navigate().to(listPageUrl);
            System.out.println("输入任意值，并回车结束：");
            scanner.next();
            System.out.println("结束");

        }catch (UnhandledAlertException e){
            if(e.getMessage().indexOf("验证码输入有误,请重新输入")>0){
                System.out.println("验证码识别错误，请重新运行代码！");
            }
            if("新增补录成功".equals(e.getAlertText())){
                System.out.println("新增补录成功!");
            }

        }
        catch (Exception e){
            e.printStackTrace();
        }finally {
            webDriver.quit();
            scanner.close();
        }

    }
    /**
     * @Author wangwei
     * @Description //TODO 获取图片验证码
     * @Date 18:01 2019-07-21
     * @Param []
     * @return java.lang.String
     **/
    public static String getValidateStr(){
        String validateStr ="";
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
        }catch (Exception e){
            e.printStackTrace();
        }
        return validateStr;
    }

    /**
     * 根据系统类型获取chromedriver
     * @return chromedriver Url
     */
    private static String getChromeDriverUrl(){
        Properties prop = System.getProperties();
        String osName = prop.getProperty("os.name");
        if (osName.startsWith("Mac OS")) {
            // 苹果
            return "src/main/resources/chromedriver";
        } else if (osName.startsWith("Windows")) {
            // windows
            return "src/main/resources/chromedriver.exe";
        } else {
            // unix or linux
            return "src/main/resources/chromedriver";
        }
    }
}
