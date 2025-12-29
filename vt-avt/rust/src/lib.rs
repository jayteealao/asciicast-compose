use jni::JNIEnv;
use jni::objects::{JClass, JByteArray};
use jni::sys::{jlong, jint, jboolean};

/// TODO: Import avt crate and create wrapper types
/// use avt::{Parser, Buffer, ...};

/// Handle to a VT instance (opaque pointer)
type VtHandle = jlong;

/// Create a new VT instance.
///
/// Returns an opaque handle to be used in subsequent calls.
///
/// # Safety
/// This function is called from JNI and must be extern "C".
#[no_mangle]
pub extern "system" fn Java_uk_adedamola_asciicast_vt_avt_AvtNative_vtNew(
    _env: JNIEnv,
    _class: JClass,
    cols: jint,
    rows: jint,
) -> VtHandle {
    // TODO: Create avt Parser/Buffer instance
    // let vt = Box::new(AvtState::new(cols as usize, rows as usize));
    // Box::into_raw(vt) as jlong

    0 // Placeholder
}

/// Free a VT instance.
///
/// # Safety
/// Handle must be valid and not used after this call.
#[no_mangle]
pub extern "system" fn Java_uk_adedamola_asciicast_vt_avt_AvtNative_vtFree(
    _env: JNIEnv,
    _class: JClass,
    handle: VtHandle,
) {
    if handle == 0 {
        return;
    }

    // TODO: Convert handle back to Box and drop
    // unsafe {
    //     let _ = Box::from_raw(handle as *mut AvtState);
    // }
}

/// Reset the VT to a new size.
///
/// # Safety
/// Handle must be valid.
#[no_mangle]
pub extern "system" fn Java_uk_adedamola_asciicast_vt_avt_AvtNative_vtReset(
    _env: JNIEnv,
    _class: JClass,
    handle: VtHandle,
    cols: jint,
    rows: jint,
) {
    if handle == 0 {
        return;
    }

    // TODO: Reset avt instance
    // unsafe {
    //     let vt = &mut *(handle as *mut AvtState);
    //     vt.reset(cols as usize, rows as usize);
    // }
}

/// Resize the VT.
///
/// # Safety
/// Handle must be valid.
#[no_mangle]
pub extern "system" fn Java_uk_adedamola_asciicast_vt_avt_AvtNative_vtResize(
    _env: JNIEnv,
    _class: JClass,
    handle: VtHandle,
    cols: jint,
    rows: jint,
) {
    if handle == 0 {
        return;
    }

    // TODO: Resize avt instance
    // unsafe {
    //     let vt = &mut *(handle as *mut AvtState);
    //     vt.resize(cols as usize, rows as usize);
    // }
}

/// Feed bytes to the VT.
///
/// # Safety
/// Handle must be valid. byte_array must be a valid JByteArray.
#[no_mangle]
pub extern "system" fn Java_uk_adedamola_asciicast_vt_avt_AvtNative_vtFeed(
    env: JNIEnv,
    _class: JClass,
    handle: VtHandle,
    byte_array: JByteArray,
) {
    if handle == 0 {
        return;
    }

    // TODO: Get bytes from JByteArray and feed to avt
    // let bytes = env.convert_byte_array(byte_array).unwrap();
    // unsafe {
    //     let vt = &mut *(handle as *mut AvtState);
    //     vt.feed(&bytes);
    // }
}

/// Capture a snapshot of the VT state.
///
/// Returns a byte array containing the encoded snapshot.
///
/// # Snapshot Format (Binary)
/// - [cols: varint][rows: varint]
/// - [cursorRow: varint][cursorCol: varint][cursorVisible: u8]
/// - [styleTableSize: varint]
/// - For each style: [fg: u32][bg: u32][attrs: u8]
/// - [lineCount: varint]
/// - For each line:
///   - [runCount: varint]
///   - For each run:
///     - [colStart: varint][textLen: varint][styleId: varint][textBytes...]
///
/// # Safety
/// Handle must be valid.
#[no_mangle]
pub extern "system" fn Java_uk_adedamola_asciicast_vt_avt_AvtNative_vtSnapshot(
    env: JNIEnv,
    _class: JClass,
    handle: VtHandle,
) -> JByteArray {
    if handle == 0 {
        return JByteArray::default();
    }

    // TODO: Encode snapshot to bytes
    // unsafe {
    //     let vt = &*(handle as *const AvtState);
    //     let snapshot_bytes = vt.encode_snapshot();
    //     env.byte_array_from_slice(&snapshot_bytes).unwrap()
    // }

    JByteArray::default()
}

/// Poll for differential update.
///
/// Returns null if no changes, otherwise a byte array with diff info.
///
/// # Diff Format (Binary)
/// - [hasDiff: u8] (0 = no diff, 1 = has diff)
/// - If has diff:
///   - [dirtyLineCount: varint]
///   - [dirtyLineIndices: varint...]
///   - [cursorChanged: u8]
///   - [resized: u8]
///
/// # Safety
/// Handle must be valid.
#[no_mangle]
pub extern "system" fn Java_uk_adedamola_asciicast_vt_avt_AvtNative_vtPollDiff(
    env: JNIEnv,
    _class: JClass,
    handle: VtHandle,
) -> JByteArray {
    if handle == 0 {
        return JByteArray::default();
    }

    // TODO: Get diff from avt (track dirty lines)
    // unsafe {
    //     let vt = &mut *(handle as *mut AvtState);
    //     if let Some(diff_bytes) = vt.poll_diff() {
    //         env.byte_array_from_slice(&diff_bytes).unwrap()
    //     } else {
    //         JByteArray::default()
    //     }
    // }

    JByteArray::default()
}

// TODO: Implement AvtState wrapper around avt types
// struct AvtState {
//     parser: avt::Parser,
//     buffer: avt::Buffer,
//     dirty_lines: HashSet<usize>,
//     last_cursor: (usize, usize),
// }
//
// impl AvtState {
//     fn new(cols: usize, rows: usize) -> Self { ... }
//     fn reset(&mut self, cols: usize, rows: usize) { ... }
//     fn resize(&mut self, cols: usize, rows: usize) { ... }
//     fn feed(&mut self, bytes: &[u8]) { ... }
//     fn encode_snapshot(&self) -> Vec<u8> { ... }
//     fn poll_diff(&mut self) -> Option<Vec<u8>> { ... }
// }
