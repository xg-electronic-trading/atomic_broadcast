package command;

import com.messages.sbe.OrdTypeEnum;
import com.messages.sbe.SideEnum;
import com.messages.sbe.StrategyEnum;
import com.messages.sbe.TimeInForceEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import schema.api.NewOrderSingleImpl;
import schema.api.PacketReader;

public class CommandReadWriteTest {

    @Test
    public void readWriteCommandTest() {
        PacketReader packetReader = new PacketReader();
        CommandBuilder cmdBuilder = new CommandBuilderImpl();

        NewOrderSingleCommandImpl cmd = cmdBuilder.createNewOrderSingle();
        cmd.parentId(1234)
                .symbol("VOD.L")
                .side(SideEnum.Buy)
                .transactTime(1_000)
                .qty(1000)
                .tif(TimeInForceEnum.Day)
                .ordType(OrdTypeEnum.Limit)
                .price(100_000L)
                .strategy(StrategyEnum.LIQSEEK)
                .exDest(5)
                .startTime(1_000)
                .endTime(1_001);

        packetReader.wrap(cmdBuilder.buffer(), 0, packetReader.offset() + cmd.encodedLength());
        NewOrderSingleImpl nosEvent = new NewOrderSingleImpl();

        nosEvent.init(packetReader);

        Assertions.assertEquals(0, nosEvent.id());
        Assertions.assertEquals(1234, nosEvent.parentId());
        Assertions.assertEquals("VOD.L", nosEvent.symbol());
        Assertions.assertEquals(SideEnum.Buy, nosEvent.side());
        Assertions.assertEquals(1_000, nosEvent.qty());
        Assertions.assertEquals(TimeInForceEnum.Day, nosEvent.tif());
        Assertions.assertEquals(OrdTypeEnum.Limit, nosEvent.ordType());
        Assertions.assertEquals(100_000, nosEvent.price());
        Assertions.assertEquals(StrategyEnum.LIQSEEK, nosEvent.strategy());
        Assertions.assertEquals(5, nosEvent.exDest());
        Assertions.assertEquals(1_000, nosEvent.startTime());
        Assertions.assertEquals(1_001, nosEvent.endTime());

    }
}
