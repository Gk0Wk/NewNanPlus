# About Cron & CronExpression

I make this wheel because bukkit's scheduler may not work in time for using tick for time. For instance, if TPS of the server is always below 20.0, then the delay of a task may take more time than expected.

Besides, many of current plugins are implemented with ticks counting.

I need some tool to help server managers to create schedule tasks in simple way. It should be punctual, and powerful. Then I think of cron, a schedule tool in linux.

I copied the syntax of cron, add a second part to support second-level schedule. The syntax looks like this:

```
[second] [minute] [hour] [dayOfMonth] [month] [dayOfWeek] (year)
```

## second

Example: 

* `1` means run at second 1.
* `3,5,8-12` means `3,5,8,9,10,11,12`
* `*` means `0-59`
* `*/4` means `0,4,8,12,16,20,24,28,32,36,40,44,48,52,56`
  * `*/2`, `*/3`, `*/4`, `*/5`, `*/6`, `*/10`, `*/12`, `*/15`, `*/20`, `*/30` is legal.
  
 ## minute
 
 Same as second.
 
 ## hour
 
 Familiar with second and minute.
 
 ## day of month
 
 Familiar with second, minute and hour, but also support some special usage:
 
 * `W` means the first weekday of this month.
 * `L` means the last day of this month.
 
 ## month
 
 Familiar with second, minute and hour, but also accept words along with numbers. For example:
 
 * `jan`, `JAN`, `Jan`, `January`, `1` are same.
 
 ## day of week
 
 Familiar with second, minute and hour, but also accept words along with numbers:
 
 * `mon`, `MON`, `Mon`, `Monday`, `1` are same.
 * `0` and `7` are both stand for Sunday.
 
 Along with some special usage:
 
 * `wed#3` means the third wednesday of this month.
 
 ## year (optional)
 
 Year part is optional. If it doesn't exist, it means `*`.

 
 # How it works
 
 Mode 1: Virtual Wheels Tick. `tickOne` to find next available time.
 
 Mode 2: Rapid Locate. Using when initializing.