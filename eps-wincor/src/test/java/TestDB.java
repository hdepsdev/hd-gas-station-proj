import com.bhz.eps.entity.Item;
import com.bhz.eps.entity.Order;
import com.bhz.eps.entity.SaleItemEntity;
import com.bhz.eps.entity.CardServiceRequest.SaleItem;
import com.bhz.eps.service.ItemService;
import com.bhz.eps.service.OrderService;
import com.bhz.eps.service.SaleItemService;
import com.bhz.eps.util.Utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by summer on 2017/01/25.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/conf/application-context.xml",
        "classpath:/conf/application-dbcfg.xml"})
public class TestDB {
    @Resource
    ItemService itemService;
    @Resource
    SaleItemService saleItemService;
    @Resource
    OrderService orderService;

    @Test
    public void testAdd() throws Exception {
        Item item = new Item();
        item.setCode("testCode");
        item.setName("testName");
        item.setCatalog(-1);
        itemService.addItem(item);

        Order order = new Order();
        order.setOrderId("testOrder");
        order.setOrderTime(new Date().getTime());
        order.setOriginalAmount(new BigDecimal(0));
        orderService.addOrder(order);

        SaleItemEntity saleItem = new SaleItemEntity();
        saleItem.setId("testId");
        saleItem.setProductCode("testCode");
        saleItem.setOrderId("testOrder");
        saleItem.setItemSeq(1);
        saleItem.setTaxCode("");
        saleItem.setAmount(new BigDecimal(0));
        saleItem.setQuantity(new BigDecimal(0));
        saleItem.setUnitMeasure("");
        saleItem.setUnitPrice(new BigDecimal(0));
        saleItemService.addSaleItem(saleItem);
    }

    @Test
    public void testGet() throws Exception {
        Item item = itemService.getItembyCode("testCode");
        Order order = orderService.getOrderbyId("201701251001011");
        List<SaleItemEntity> list = saleItemService.getSaleItemsbyOrderId("201701251001012");
        list.size();
    }
    
    @Test
    public void testOrderService1() {
    	Order order = orderService.getOrderWithSaleItemsById("1");
    	Set<SaleItemEntity> orderItems = order.getOrderItems();
    	System.out.println("order id : " + order.getOrderId() + "\tOriginal Amount" + order.getOriginalAmount());
    	System.out.println("============== Details ==============");
    	for(SaleItemEntity sie:orderItems){
    		System.out.println("Sale Item id: " + sie.getId() + "\tQuantity: " + sie.getQuantity() + "\tAmount: " + sie.getAmount() +"\tPrice: "+ sie.getUnitPrice() + "\tItem Name: " + sie.getItemName());
    	}
    }
}
