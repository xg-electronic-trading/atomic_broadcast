package transport;

import atomic_broadcast.client.CommandProcessor;
import atomic_broadcast.client.CommandProcessorImpl;
import atomic_broadcast.client.CommandPublisher;
import atomic_broadcast.client.NoOpCommandValidator;
import atomic_broadcast.utils.Action;
import atomic_broadcast.utils.EventReaderType;
import atomic_broadcast.utils.InstanceInfo;
import com.messages.sbe.OrdTypeEnum;
import com.messages.sbe.SideEnum;
import com.messages.sbe.StrategyEnum;
import com.messages.sbe.TimeInForceEnum;
import command.CommandBuilder;
import command.CommandBuilderImpl;
import command.NewOrderSingleCommandImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import utils.SequencerTestFixture;

import static atomic_broadcast.utils.Action.CommandSent;
import static atomic_broadcast.utils.App.AlgoContainer;

public class SingleHostSequencerRoundTripTest {

    private SequencerTestFixture fixture;

    @BeforeEach
    public void before() {
        fixture = new SequencerTestFixture();
    }

    @ParameterizedTest
    @EnumSource(EventReaderType.class)
    public void singleMessageRoundTrip(EventReaderType eventReaderType) {
        fixture.before(eventReaderType, 1);
        fixture.start();
        fixture.setLeader(1);
        fixture.pollStandAloneSequencer();
        /**
         * Add logic to push a message through sequencer via
         * a command builder and assert it round trips through
         * sequencer.
         */
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

        boolean sent = fixture.sendCommand(nos);
        Assertions.assertTrue(sent);
        fixture.pollUntilCommandIdAcked(fixture.eventReaders, id);
    }

    @AfterEach
    public void after() {
        fixture.after();
    }
}
