/*
 * This file is generated by jOOQ.
 */
package brs.schema.tables.records;


import brs.schema.tables.IndirectIncoming;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class IndirectIncomingRecord extends UpdatableRecordImpl<IndirectIncomingRecord> implements Record4<Long, Long, Long, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>DB.indirect_incoming.db_id</code>.
     */
    public void setDbId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>DB.indirect_incoming.db_id</code>.
     */
    public Long getDbId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>DB.indirect_incoming.account_id</code>.
     */
    public void setAccountId(Long value) {
        set(1, value);
    }

    /**
     * Getter for <code>DB.indirect_incoming.account_id</code>.
     */
    public Long getAccountId() {
        return (Long) get(1);
    }

    /**
     * Setter for <code>DB.indirect_incoming.transaction_id</code>.
     */
    public void setTransactionId(Long value) {
        set(2, value);
    }

    /**
     * Getter for <code>DB.indirect_incoming.transaction_id</code>.
     */
    public Long getTransactionId() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>DB.indirect_incoming.height</code>.
     */
    public void setHeight(Integer value) {
        set(3, value);
    }

    /**
     * Getter for <code>DB.indirect_incoming.height</code>.
     */
    public Integer getHeight() {
        return (Integer) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<Long, Long, Long, Integer> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<Long, Long, Long, Integer> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<Long> field1() {
        return IndirectIncoming.INDIRECT_INCOMING.DB_ID;
    }

    @Override
    public Field<Long> field2() {
        return IndirectIncoming.INDIRECT_INCOMING.ACCOUNT_ID;
    }

    @Override
    public Field<Long> field3() {
        return IndirectIncoming.INDIRECT_INCOMING.TRANSACTION_ID;
    }

    @Override
    public Field<Integer> field4() {
        return IndirectIncoming.INDIRECT_INCOMING.HEIGHT;
    }

    @Override
    public Long component1() {
        return getDbId();
    }

    @Override
    public Long component2() {
        return getAccountId();
    }

    @Override
    public Long component3() {
        return getTransactionId();
    }

    @Override
    public Integer component4() {
        return getHeight();
    }

    @Override
    public Long value1() {
        return getDbId();
    }

    @Override
    public Long value2() {
        return getAccountId();
    }

    @Override
    public Long value3() {
        return getTransactionId();
    }

    @Override
    public Integer value4() {
        return getHeight();
    }

    @Override
    public IndirectIncomingRecord value1(Long value) {
        setDbId(value);
        return this;
    }

    @Override
    public IndirectIncomingRecord value2(Long value) {
        setAccountId(value);
        return this;
    }

    @Override
    public IndirectIncomingRecord value3(Long value) {
        setTransactionId(value);
        return this;
    }

    @Override
    public IndirectIncomingRecord value4(Integer value) {
        setHeight(value);
        return this;
    }

    @Override
    public IndirectIncomingRecord values(Long value1, Long value2, Long value3, Integer value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached IndirectIncomingRecord
     */
    public IndirectIncomingRecord() {
        super(IndirectIncoming.INDIRECT_INCOMING);
    }

    /**
     * Create a detached, initialised IndirectIncomingRecord
     */
    public IndirectIncomingRecord(Long dbId, Long accountId, Long transactionId, Integer height) {
        super(IndirectIncoming.INDIRECT_INCOMING);

        setDbId(dbId);
        setAccountId(accountId);
        setTransactionId(transactionId);
        setHeight(height);
    }
}
