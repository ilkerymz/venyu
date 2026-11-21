export default function Loading() {
    return (
        <div aria-busy aria-label="Yükleniyor" className="grid max-w-3xl gap-6">
            <div className="h-40 rounded-xl bg-muted motion-safe:animate-pulse" />
            <div className="h-56 rounded-xl bg-muted motion-safe:animate-pulse" />
        </div>
    );
}
