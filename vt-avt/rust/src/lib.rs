use jni::JNIEnv;
use jni::objects::{JClass, JByteArray};
use jni::sys::{jlong, jint};
use std::collections::HashSet;
use avt::{Vt, Pen};

/// Wrapper around avt::Vt with dirty tracking
struct AvtState {
    vt: Vt,
    dirty_lines: HashSet<usize>,
    cursor_changed: bool,
    resized: bool,
}

impl AvtState {
    fn new(cols: usize, rows: usize) -> Self {
        AvtState {
            vt: Vt::builder()
                .size(cols, rows)
                .build(),
            dirty_lines: (0..rows).collect(),
            cursor_changed: true,
            resized: false,
        }
    }

    fn reset(&mut self, cols: usize, rows: usize) {
        self.vt = Vt::builder()
            .size(cols, rows)
            .build();
        self.dirty_lines = (0..rows).collect();
        self.cursor_changed = true;
        self.resized = true;
    }

    fn resize(&mut self, cols: usize, rows: usize) {
        self.vt.feed_str(&format!("\x1b[8;{};{}t", rows, cols));
        self.dirty_lines = (0..rows).collect();
        self.cursor_changed = true;
        self.resized = true;
    }

    fn feed(&mut self, bytes: &[u8]) {
        // Mark all lines as potentially dirty for simplicity
        // A more optimized version would track actual changes
        for row in 0..self.vt.lines().count().min(self.vt.size().1) {
            self.dirty_lines.insert(row);
        }

        for &byte in bytes {
            self.vt.feed(byte as char);
        }

        self.cursor_changed = true;
    }

    fn encode_snapshot(&self) -> Vec<u8> {
        let mut buf = Vec::new();
        let size = self.vt.size();

        // Write size
        write_varint(&mut buf, size.0);
        write_varint(&mut buf, size.1);

        // Write cursor
        let cursor = self.vt.cursor();
        write_varint(&mut buf, cursor.col);
        write_varint(&mut buf, cursor.row);
        buf.push(if cursor.visible { 1 } else { 0 });

        // Encode lines
        for line in self.vt.lines().take(size.1) {
            encode_line(&mut buf, line);
        }

        buf
    }

    fn poll_diff(&mut self) -> Option<Vec<u8>> {
        if self.dirty_lines.is_empty() && !self.cursor_changed && !self.resized {
            return None;
        }

        let mut buf = Vec::new();
        buf.push(1); // has diff

        // Write dirty line count and indices
        write_varint(&mut buf, self.dirty_lines.len());
        let mut sorted: Vec<_> = self.dirty_lines.iter().copied().collect();
        sorted.sort_unstable();
        for idx in sorted {
            write_varint(&mut buf, idx);
        }

        // Write cursor changed flag
        buf.push(if self.cursor_changed { 1 } else { 0 });

        // Write resize flag
        buf.push(if self.resized { 1 } else { 0 });

        // Clear dirty state
        self.dirty_lines.clear();
        self.cursor_changed = false;
        self.resized = false;

        Some(buf)
    }
}

// Helper functions for encoding
fn write_varint(buf: &mut Vec<u8>, mut value: usize) {
    loop {
        let mut byte = (value & 0x7F) as u8;
        value >>= 7;
        if value != 0 {
            byte |= 0x80;
        }
        buf.push(byte);
        if value == 0 {
            break;
        }
    }
}

fn encode_line(buf: &mut Vec<u8>, line: &avt::Line) {
    let cells = line.cells();
    let mut runs = Vec::new();
    let mut current_pen: Option<Pen> = None;
    let mut current_text = String::new();
    let mut current_start = 0;

    for (col, cell) in cells.iter().enumerate() {
        if current_pen.as_ref() != Some(cell.pen()) {
            if !current_text.is_empty() {
                runs.push((current_start, current_text.clone(), current_pen.unwrap()));
                current_text.clear();
            }
            current_pen = Some(*cell.pen());
            current_start = col;
        }
        current_text.push(cell.char());
    }

    if !current_text.is_empty() {
        runs.push((current_start, current_text, current_pen.unwrap()));
    }

    // Write run count
    write_varint(buf, runs.len());

    // Write each run
    for (col_start, text, pen) in runs {
        write_varint(buf, col_start);
        write_varint(buf, text.len());
        encode_pen(buf, pen);
        buf.extend_from_slice(text.as_bytes());
    }
}

fn encode_pen(buf: &mut Vec<u8>, pen: Pen) {
    // Encode foreground color
    match pen.foreground() {
        Some(avt::Color::Indexed(idx)) => {
            buf.push(0);
            buf.push(idx);
        }
        Some(avt::Color::RGB(rgb)) => {
            buf.push(1);
            buf.push(rgb.r);
            buf.push(rgb.g);
            buf.push(rgb.b);
        }
        None => {
            buf.push(2);
        }
    }

    // Encode background color
    match pen.background() {
        Some(avt::Color::Indexed(idx)) => {
            buf.push(0);
            buf.push(idx);
        }
        Some(avt::Color::RGB(rgb)) => {
            buf.push(1);
            buf.push(rgb.r);
            buf.push(rgb.g);
            buf.push(rgb.b);
        }
        None => {
            buf.push(2);
        }
    }

    // Encode attributes
    let mut attrs = 0u8;
    if pen.is_bold() { attrs |= 0x01; }
    if pen.is_italic() { attrs |= 0x02; }
    if pen.is_underline() { attrs |= 0x04; }
    if pen.is_strikethrough() { attrs |= 0x08; }
    if pen.is_blink() { attrs |= 0x10; }
    if pen.is_inverse() { attrs |= 0x20; }
    buf.push(attrs);
}

// JNI functions

type VtHandle = jlong;

#[no_mangle]
pub extern "system" fn Java_uk_adedamola_asciicast_vt_avt_AvtNative_vtNew(
    _env: JNIEnv,
    _class: JClass,
    cols: jint,
    rows: jint,
) -> VtHandle {
    let vt = Box::new(AvtState::new(cols as usize, rows as usize));
    Box::into_raw(vt) as jlong
}

#[no_mangle]
pub extern "system" fn Java_uk_adedamola_asciicast_vt_avt_AvtNative_vtFree(
    _env: JNIEnv,
    _class: JClass,
    handle: VtHandle,
) {
    if handle == 0 {
        return;
    }

    unsafe {
        let _ = Box::from_raw(handle as *mut AvtState);
    }
}

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

    unsafe {
        let vt = &mut *(handle as *mut AvtState);
        vt.reset(cols as usize, rows as usize);
    }
}

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

    unsafe {
        let vt = &mut *(handle as *mut AvtState);
        vt.resize(cols as usize, rows as usize);
    }
}

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

    let bytes = match env.convert_byte_array(byte_array) {
        Ok(b) => b,
        Err(_) => return,
    };

    unsafe {
        let vt = &mut *(handle as *mut AvtState);
        vt.feed(&bytes);
    }
}

#[no_mangle]
pub extern "system" fn Java_uk_adedamola_asciicast_vt_avt_AvtNative_vtSnapshot<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    handle: VtHandle,
) -> JByteArray<'a> {
    if handle == 0 {
        return JByteArray::default();
    }

    unsafe {
        let vt = &*(handle as *const AvtState);
        let snapshot_bytes = vt.encode_snapshot();
        match env.byte_array_from_slice(&snapshot_bytes) {
            Ok(arr) => arr,
            Err(_) => JByteArray::default(),
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_uk_adedamola_asciicast_vt_avt_AvtNative_vtPollDiff<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    handle: VtHandle,
) -> JByteArray<'a> {
    if handle == 0 {
        return JByteArray::default();
    }

    unsafe {
        let vt = &mut *(handle as *mut AvtState);
        if let Some(diff_bytes) = vt.poll_diff() {
            match env.byte_array_from_slice(&diff_bytes) {
                Ok(arr) => arr,
                Err(_) => JByteArray::default(),
            }
        } else {
            JByteArray::default()
        }
    }
}
