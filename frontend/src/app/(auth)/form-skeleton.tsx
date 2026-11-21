/** URL'deki token/parametre okunurken gösterilen yer tutucu; başlık beklemeden görünür. */
export function FormSkeleton() {
    return (
        <div aria-busy aria-label="Yükleniyor" className="grid gap-4">
            <div className="h-11 rounded-md bg-muted motion-safe:animate-pulse" />
            <div className="h-11 w-40 rounded-md bg-muted motion-safe:animate-pulse" />
        </div>
    );
}
