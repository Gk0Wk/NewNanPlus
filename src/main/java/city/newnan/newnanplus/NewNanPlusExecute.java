package city.newnan.newnanplus;

import city.newnan.newnanplus.cron.CronExpression;

import java.util.Scanner;

/**
 * 只有直接运行这个jar才会跑到这里来
 */
public class NewNanPlusExecute {
    /**
     * 只有直接运行这个jar才会跑到这里来
     * @param args 命令行参数
     */
    public static void main(String[] args){
        Scanner input = new Scanner(System.in);
        String str = input.nextLine();
        CronExpression expression = new CronExpression(str);

        System.out.printf("Next: %d\n", expression.getNextTime());

        // System.out.println("[NewNanCity] 这不是一个可执行的jar文件，请将其放在plugins文件夹下。");
        System.exit(0);
        String a;
    }
}
