use spacetimedb::{Identity, ReducerContext, Table, Timestamp};

#[spacetimedb::table(accessor = user, public)]
pub struct User {
    #[primary_key]
    identity: Identity,
    name: Option<String>,
    online: bool,
}

#[spacetimedb::table(accessor = message, public)]
pub struct Message {
    #[auto_inc]
    #[primary_key]
    id: u64,
    sender: Identity,
    sent: Timestamp,
    text: String,
}

/// A simple note table — used to test onDelete and filtered subscriptions.
#[spacetimedb::table(accessor = note, public)]
pub struct Note {
    #[auto_inc]
    #[primary_key]
    id: u64,
    owner: Identity,
    content: String,
    tag: String,
}

fn validate_name(name: String) -> Result<String, String> {
    if name.is_empty() {
        Err("Names must not be empty".to_string())
    } else {
        Ok(name)
    }
}

#[spacetimedb::reducer]
pub fn set_name(ctx: &ReducerContext, name: String) -> Result<(), String> {
    let name = validate_name(name)?;
    if let Some(user) = ctx.db.user().identity().find(ctx.sender()) {
        log::info!("User {} sets name to {name}", ctx.sender());
        ctx.db.user().identity().update(User {
            name: Some(name),
            ..user
        });
        Ok(())
    } else {
        Err("Cannot set name for unknown user".to_string())
    }
}

fn validate_message(text: String) -> Result<String, String> {
    if text.is_empty() {
        Err("Messages must not be empty".to_string())
    } else {
        Ok(text)
    }
}

#[spacetimedb::reducer]
pub fn send_message(ctx: &ReducerContext, text: String) -> Result<(), String> {
    let text = validate_message(text)?;
    log::info!("User {}: {text}", ctx.sender());
    ctx.db.message().insert(Message {
        id: 0,
        sender: ctx.sender(),
        text,
        sent: ctx.timestamp,
    });
    Ok(())
}

#[spacetimedb::reducer]
pub fn delete_message(ctx: &ReducerContext, message_id: u64) -> Result<(), String> {
    if let Some(msg) = ctx.db.message().id().find(message_id) {
        if msg.sender != ctx.sender() {
            return Err("Cannot delete another user's message".to_string());
        }
        ctx.db.message().id().delete(message_id);
        log::info!("User {} deleted message {message_id}", ctx.sender());
        Ok(())
    } else {
        Err("Message not found".to_string())
    }
}

#[spacetimedb::reducer]
pub fn add_note(ctx: &ReducerContext, content: String, tag: String) -> Result<(), String> {
    if content.is_empty() {
        return Err("Note content must not be empty".to_string());
    }
    ctx.db.note().insert(Note {
        id: 0,
        owner: ctx.sender(),
        content,
        tag,
    });
    Ok(())
}

#[spacetimedb::reducer]
pub fn delete_note(ctx: &ReducerContext, note_id: u64) -> Result<(), String> {
    if let Some(note) = ctx.db.note().id().find(note_id) {
        if note.owner != ctx.sender() {
            return Err("Cannot delete another user's note".to_string());
        }
        ctx.db.note().id().delete(note_id);
        Ok(())
    } else {
        Err("Note not found".to_string())
    }
}

#[spacetimedb::reducer(init)]
pub fn init(_ctx: &ReducerContext) {}

#[spacetimedb::reducer(client_connected)]
pub fn identity_connected(ctx: &ReducerContext) {
    if let Some(user) = ctx.db.user().identity().find(ctx.sender()) {
        ctx.db.user().identity().update(User { online: true, ..user });
    } else {
        ctx.db.user().insert(User {
            name: None,
            identity: ctx.sender(),
            online: true,
        });
    }
}

#[spacetimedb::reducer(client_disconnected)]
pub fn identity_disconnected(ctx: &ReducerContext) {
    if let Some(user) = ctx.db.user().identity().find(ctx.sender()) {
        ctx.db.user().identity().update(User { online: false, ..user });
    } else {
        log::warn!("Disconnect event for unknown user with identity {:?}", ctx.sender());
    }
}
