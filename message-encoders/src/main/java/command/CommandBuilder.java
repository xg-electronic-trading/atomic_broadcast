package command;

import org.agrona.DirectBuffer;

public interface CommandBuilder {

    NewOrderSingleCommand createNewOrderSingle();

    DirectBuffer buffer();
}
