import QRCode from "qrcode";

/**
 * Bilet kodunu QR olarak çizer (ADR-010: backend sadece kodu döner, QR'ı frontend çizer).
 * `QRCode.create` senkron çalışır; sunucuda ve tarayıcıda aynı SVG üretilir.
 */
export function TicketQr({ code, className }: { code: string; className?: string }) {
    const { modules } = QRCode.create(code, { errorCorrectionLevel: "M" });
    const size = modules.size;
    const quiet = 2;
    let path = "";
    for (let y = 0; y < size; y++) {
        for (let x = 0; x < size; x++) {
            if (modules.get(y, x)) path += `M${x + quiet} ${y + quiet}h1v1h-1z`;
        }
    }
    const total = size + quiet * 2;
    return (
        <svg
            viewBox={`0 0 ${total} ${total}`}
            role="img"
            aria-label={`Bilet QR kodu: ${code}`}
            shapeRendering="crispEdges"
            className={className}
        >
            <rect width={total} height={total} fill="#fff" />
            <path d={path} fill="#15172e" />
        </svg>
    );
}
