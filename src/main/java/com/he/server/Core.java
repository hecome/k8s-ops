package com.he.server;



import com.he.utils.HeUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Core {

    private static final String list="kubectl top pods -n ingress-nginx";
    private static final String single="kubectl get pod %s -n ingress-nginx -o=jsonpath='{.spec.containers[*].resources.limits.memory}'";
    private static final int warningRate=75;

    /**
     * 获取pods
     * @throws Exception
     */
    public void listContainers()throws Exception{
        System.out.println("==================================================================================================");
        Process process=Runtime.getRuntime().exec(list);
        int value=process.waitFor();
        System.out.printf("%s %s exec linux shell: %s ,value is %s \r\n",this.getTime(),Thread.currentThread().getName(),list,value);
        if(value==0){
            try (
                    BufferedReader inputStream=new BufferedReader(new InputStreamReader(process.getInputStream(),"utf-8"));
                    ){
                String temp=null;
                System.out.println("------------------------------------------------------------------------------------------------");
                while ((temp=inputStream.readLine())!=null){
                    System.out.printf("%s %s linux shell result is : %s \r\n",this.getTime(),Thread.currentThread().getName(),temp);
                    String arr[]=temp.split("\\s+");
                    checkSingle(arr);
                }
            }catch (Exception e){
                throw e;
            }
        }
        process.destroy();
    }

    /**
     * 检查单个pod
     * @param arr
     * @throws Exception
     */
    public void checkSingle(String[] arr)throws Exception{
        String shell=String.format(single,arr[0]);
        System.out.printf("%s %s linux shell is: %s \r\n",this.getTime(),Thread.currentThread().getName(),shell);
        Process process=Runtime.getRuntime().exec(shell);
        process.waitFor();
        try (
               InputStream inputStream=process.getInputStream();
                ){
            byte[] buf=new byte[1024];
            inputStream.read(buf);
            String limitMemory=new String(buf).replace("\'","").trim();
            List<String> list=this.addInfoToList(arr,limitMemory);
            System.out.printf("%s %s container : %s , cpu : %s ,memory : %s ,limit : %s,rate : %s \r\n",this.getTime(),Thread.currentThread().getName(),list.get(0),list.get(1),list.get(2),list.get(3),list.get(4));
        }catch (Exception e){
            throw e;
        }
        System.out.println("------------------------------------------------------------------------------------------------");
        process.destroy();
    }

    /**
     * 信息转化
     * @param arr
     * @param limitMemory
     * @return
     * @throws Exception
     */
    private List<String> addInfoToList(String[] arr,String limitMemory)throws Exception{
        ArrayList<String> list=new ArrayList<>();
        list.addAll(Arrays.asList(arr));
        String limit=null;
        if(limitMemory.contains("Gi")){
            limit=Integer.parseInt(limitMemory.replace("Gi",""))*1024+"";
        }else if(limitMemory.contains("Mi")){
            limit=limitMemory.replace("Mi","");
        }
        System.out.println(this.getTime()+" "+Thread.currentThread().getName()+" container limit is : "+limit);
        boolean result=(limit==null) ? list.add(null) : list.add(limit+"Mi");
        // 百分比
        if(list.get(3)!=null){
            int v = (int) ((new BigDecimal((float) Integer.parseInt(list.get(2).replace("Mi","")) / Integer.parseInt(limit)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue())*100);
            list.add(v+"%");
            if(v>warningRate){
                String detail=String.format("WARNING \r\ncontainer : %s \r\ncpu : %s \r\nmemory : %s \r\nlimit : %s \r\nrate : %s \r\n @15755356390",list.get(0),list.get(1),list.get(2),list.get(3),list.get(4));
                this.noticeMessage(detail);
            }
        }else {
            list.add(null);
        }
        return list;
    }

    /**
     * 发送警告信息
     * @param detail
     * @throws Exception
     */
    private void noticeMessage(String detail)throws Exception{
        if(this.isSafeTime()){
            try (
                    InputStream inputStream= Core.class.getClassLoader().getResourceAsStream("notice-message");
            ){
                byte[] buf=new byte[1024];
                inputStream.read(buf);
                String template=new String(buf,"utf-8");
                String content=String.format(template,detail);
                HeUtils.httpRequestForPOST("https://oapi.dingtalk.com/robot/send?access_token=8bf3f25075f581d25f0b8e0a8d51909b1ec1fa06ceda530ca79b02600524dabe",content);
            }catch (Exception e){
                throw e;
            }
        }
    }

    /**
     * 获取当前时间
     * @return
     */
    private String getTime(){
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String result=simpleDateFormat.format(new Date());
        return result;
    }

    /**
     * 在晚上10点过后，早上9点之前，都不通知
     * @return
     */
    private boolean isSafeTime(){
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("HH");
        String result=simpleDateFormat.format(new Date());
        Integer hour=Integer.parseInt(result);
        if(hour>22 || hour<9){
            return false;
        }
        return true;
    }


}
