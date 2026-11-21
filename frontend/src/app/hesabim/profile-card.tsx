import { BadgeCheck, Building2 } from "lucide-react";
import Link from "next/link";
import type { MeResponse } from "@/api/generated/model";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { formatPhone } from "@/lib/phone";
import { LogoutButton } from "./account-actions";
import { EmailRow } from "./email-row";
import { PhoneRow } from "./phone-row";

const ROLE_LABELS: Record<string, string> = {
    PARTICIPANT: "Katılımcı",
    ORGANIZER: "Organizatör",
    VENUE_OWNER: "Mekan sahibi",
};

/** Hesabım'ın sol sütunu: kim olduğun, doğrulama durumların, mekanların ve çıkış. */
export function ProfileCard({ profile }: { profile: MeResponse }) {
    const roles = profile.roles ?? [];
    return (
        <section aria-labelledby="profile-heading" className="rounded-xl border bg-card">
            <div className="p-6">
                <h1
                    id="profile-heading"
                    className="text-3xl leading-tight font-bold tracking-[-0.02em] break-words [font-stretch:85%]"
                >
                    {profile.displayName}
                </h1>
                <div className="mt-3 flex flex-wrap gap-1.5">
                    {roles.map((role) => (
                        <Badge key={role} variant="secondary">
                            {ROLE_LABELS[role] ?? role}
                        </Badge>
                    ))}
                </div>
            </div>

            <dl className="divide-y border-t">
                <div className="px-6 py-4">
                    <dt className="text-sm font-medium">E-posta</dt>
                    <dd className="mt-1">
                        <span className="flex items-center gap-1.5 break-all">
                            {profile.email}
                            {profile.emailVerified && <Verified />}
                        </span>
                        {!profile.emailVerified && <EmailRow />}
                    </dd>
                </div>
                <div className="px-6 py-4">
                    <dt className="text-sm font-medium">Telefon</dt>
                    <dd className="mt-1">
                        {profile.phoneVerified && profile.phone ? (
                            <span className="flex items-center gap-1.5 nums">
                                {formatPhone(profile.phone)}
                                <Verified />
                            </span>
                        ) : (
                            <PhoneRow current={profile.phone} />
                        )}
                    </dd>
                </div>
            </dl>

            <div className="grid gap-2 border-t p-6">
                {roles.includes("VENUE_OWNER") && (
                    <Button variant="outline" className="justify-start" asChild>
                        <Link href="/mekan">
                            <Building2 aria-hidden />
                            Mekanlarım
                        </Link>
                    </Button>
                )}
                <LogoutButton />
            </div>
        </section>
    );
}

function Verified() {
    return (
        <span className="inline-flex shrink-0 items-center gap-1 text-sm font-medium text-success">
            <BadgeCheck className="size-4" aria-hidden />
            Doğrulandı
        </span>
    );
}
