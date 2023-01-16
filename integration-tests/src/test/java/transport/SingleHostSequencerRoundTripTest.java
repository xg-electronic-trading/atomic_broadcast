package transport;

import atomic_broadcast.client.CommandProcessor;
import atomic_broadcast.client.CommandProcessorImpl;
import atomic_broadcast.client.CommandPublisher;
import atomic_broadcast.client.NoOpCommandValidator;
import atomic_broadcast.utils.Action;
import com.messages.sbe.OrdTypeEnum;
import com.messages.sbe.SideEnum;
import com.messages.sbe.StrategyEnum;
import com.messages.sbe.TimeInForceEnum;
import command.CommandBuilder;
import command.CommandBuilderImpl;
import command.NewOrderSingleCommandImpl;
import org.junit.jupiter.api.*;
import utils.SequencerTestFixture;

import static atomic_broadcast.utils.Action.CommandSent;

public class SingleHostSequencerRoundTripTest {

    private SequencerTestFixture fixture;

    @BeforeEach
    public void before() {
        fixture = new SequencerTestFixture();
        fixture.before();
    }

    @Disabled
    public void singleMessageRoundTrip() {
        /**
         * Add logic to push a message through sequencer via
         * a command builder and assert it round trips through
         * sequencer.
         */
        CommandPublisher cmdPublisher = fixture.cmdPublisher();
        CommandProcessor cmdProcessor = new CommandProcessorImpl(cmdPublisher, new NoOpCommandValidator());
        CommandBuilder cmdBuilder = new CommandBuilderImpl();
        NewOrderSingleCommandImpl nos = cmdBuilder.createNewOrderSingle();
        long id = 1;
        nos.id(id)
                .parentId(1234)
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

        Action action = cmdProcessor.send(nos);
        Assertions.assertEquals(CommandSent, action);
        fixture.pollUntilCommandAcked(id);
    }

    @AfterEach
    public void after() {
        fixture.after();
    }
}
