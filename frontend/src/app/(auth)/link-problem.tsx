import Link from "next/link";
import { Button } from "@/components/ui/button";

/** E-posta bağlantısı token'sız açıldığında. */
export function MissingTokenNotice({
    retryHref,
    retryLabel,
}: {
    retryHref: string;
    retryLabel: string;
}) {
    return (
        <div className="grid gap-6">
            <p className="leading-relaxed text-muted-foreground">
                Bu bağlantı eksik görünüyor. E-postadaki bağlantının tamamını kopyaladığından emin
                ol ya da yeni bir bağlantı iste.
            </p>
            <Button variant="outline" asChild className="justify-self-start">
                <Link href={retryHref}>{retryLabel}</Link>
            </Button>
        </div>
    );
}
