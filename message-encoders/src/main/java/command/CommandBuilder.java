package command;

import org.agrona.DirectBuffer;

public interface CommandBuilder {

    NewOrderSingleCommandImpl createNewOrderSingle();

    DirectBuffer buffer();
}
