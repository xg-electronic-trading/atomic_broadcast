package orderstate;

public interface OrderStateSetter {

    void setId(long id);

    void setPrice(long price);

    void setQuantity(long quantity);

    void setMsgSeqNum(long msgSeqNum);
}
