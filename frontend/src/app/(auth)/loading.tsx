export default function Loading() {
    return (
        <div aria-busy aria-label="Yükleniyor" className="space-y-4">
            <div className="h-9 w-48 rounded-md bg-muted motion-safe:animate-pulse" />
            <div className="h-40 rounded-md bg-muted motion-safe:animate-pulse" />
        </div>
    );
}
