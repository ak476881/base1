package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Struct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    UserMapper userMapper;

    @Autowired
    WorkspaceService workspaceService;

    //统计营业额
    @Override
    public TurnoverReportVO getTurnover(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList=new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        String s = StringUtils.join(dateList, ",");
        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder().dateList(s).build();

        List<Double> turnoverList=new ArrayList<>();
        for (LocalDate localDate : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            Map map=new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);

            Double  turnover= orderMapper.getByMap(map);
              turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        String s1 = StringUtils.join(turnoverList,",");
        turnoverReportVO.setTurnoverList(s1);
        return turnoverReportVO;
    }

    @Override
    public UserReportVO getUser(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList=new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> newUserList=new ArrayList<>();
        List<Integer> totalUserList=new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map=new HashMap();
            map.put("end",endTime);
            Integer totalUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);
            map.put("begin",beginTime);
            Integer newUser=userMapper.countByMap(map);
            newUserList.add(newUser);
        }


        return UserReportVO.builder().dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,",")).build();
    }

    @Override
    public OrderReportVO getOrders(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList=new ArrayList<>();
        List<Integer>  validOrderCountList=new ArrayList<>();
        List<Integer>  OrderCountList=new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map=new HashMap();
            map.put("end",endTime);
            map.put("begin",beginTime);
            Integer orderCount = orderMapper.countByMap(map);
            OrderCountList.add(orderCount);

            map.put("status",Orders.COMPLETED);
            Integer validCount = orderMapper.countByMap(map);
            validOrderCountList.add(validCount);

        }

        Integer totalOrderCount = OrderCountList.stream().reduce(Integer::sum).get();

        Integer validTotal = validOrderCountList.stream().reduce(Integer::sum).get();

        Double orderCompletionRate=0.0;

        if(totalOrderCount!=0){
             orderCompletionRate=validTotal.doubleValue()/totalOrderCount;
        }

        OrderReportVO orderReportVO = OrderReportVO.builder().dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(OrderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validTotal)
                .orderCompletionRate(orderCompletionRate)
                .build();
        return orderReportVO;
    }

    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {


        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        SalesTop10ReportVO salesTop10ReportVO = SalesTop10ReportVO.builder().nameList(nameList)
                .numberList(numberList).build();

        return salesTop10ReportVO;
    }

    @Override
    public void export(HttpServletResponse response) {

       LocalDate dateBegin=LocalDate.now().minusDays(-30);
       LocalDate dateEnd=LocalDate.now().minusDays(-1);
        LocalDateTime begin = LocalDateTime.of(dateBegin, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(dateEnd, LocalTime.MAX);

        BusinessDataVO businessDataVO = workspaceService.getBusinessData(begin, end);

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet1 = workbook.getSheet("sheet1");
             sheet1.getRow(1).getCell(1).setCellValue("时间："+dateBegin+"至"+dateEnd);

             sheet1.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover());
             sheet1.getRow(3).getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
             sheet1.getRow(3).getCell(6).setCellValue(businessDataVO.getNewUsers());
             sheet1.getRow(4).getCell(2).setCellValue(businessDataVO.getValidOrderCount());
             sheet1.getRow(4).getCell(4).setCellValue(businessDataVO.getUnitPrice());


            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                BusinessDataVO businessData = workspaceService.
                        getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                XSSFRow row = sheet1.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(4).setCellValue(businessData.getValidOrderCount());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }


            ServletOutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);

            outputStream.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
