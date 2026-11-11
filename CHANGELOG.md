# Changelog

## 1.0.0

First public release.

- `CardScannerScreen`: self-contained Compose card scanner — camera permission flow, CameraX
  preview, tap to focus, torch, replaceable overlay.
- `CardAnalyzer`: standalone CameraX analyzer with frame voting, for apps that bring their own
  camera pipeline.
- Card number extraction with OCR character repair, row reconstruction, Luhn and IIN validation, and
  a reversed-row retry.
- Expiry date extraction for both `YY/MM` (Solar Hijri) and `MM/YY`.
- English and Persian strings, RTL ready, themed from the ambient `MaterialTheme`.
