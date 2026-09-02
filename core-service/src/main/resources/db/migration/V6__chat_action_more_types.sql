ALTER TABLE chat_action DROP CONSTRAINT chat_action_type_chk;

ALTER TABLE chat_action ADD CONSTRAINT chat_action_type_chk
    CHECK (type IN (
        'CREATE_PANTRY_ITEM',
        'UPDATE_PANTRY_ITEM',
        'DELETE_PANTRY_ITEM',
        'CONSUME_PANTRY_ITEM'
    ));
