import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { isApiError } from "@/api/api-error";
import { me } from "@/api/generated/me-controller/me-controller";
import { authHeaders } from "@/auth/server";
import { Notice } from "@/components/form-parts";
import { SiteHeader } from "@/components/site-header";
import { Badge } from "@/components/ui/badge";
import { BecomeOrganizerForm, LogoutButton } from "./account-actions";

export const metadata: Metadata = { title: "Hesabım", robots: { index: false } };

const ROLE_LABELS: Record<string, string> = {
    PARTICIPANT: "Katılımcı",
    ORGANIZER: "Organizatör",
    VENUE_OWNER: "Mekan sahibi",
};

type Props = { searchParams: Promise<{ hosgeldin?: string }> };

export default async function AccountPage({ searchParams }: Props) {
    const { hosgeldin } = await searchParams;
    let profile;
    try {
        ({ data: profile } = await me({ headers: await authHeaders() }));
    } catch (error) {
        if (isApiError(error, 401)) redirect("/giris?next=/hesabim");
        throw error;
    }
    const roles = profile.roles ?? [];
    const isOrganizer = roles.includes("ORGANIZER");

    return (
        <>
            <SiteHeader />
            <main className="mx-auto max-w-3xl px-5 pt-6 pb-20 sm:px-8">
                {hosgeldin && (
                    <div className="mb-8">
                        <Notice>
                            Hesabın hazır. Doğrulama bağlantısını {profile.email} adresine
                            gönderdik.
                        </Notice>
                    </div>
                )}

                <h1 className="text-4xl font-bold tracking-[-0.02em] [font-stretch:85%] sm:text-5xl">
                    {profile.displayName}
                </h1>
                <div className="mt-4 flex flex-wrap gap-2">
                    {roles.map((role) => (
                        <Badge key={role} variant="secondary">
                            {ROLE_LABELS[role] ?? role}
                        </Badge>
                    ))}
                </div>

                <section className="mt-10 rounded-xl border bg-card p-6">
                    <h2 className="text-xl font-semibold">Hesap bilgileri</h2>
                    <dl className="mt-4 grid gap-4 sm:grid-cols-[10rem_1fr]">
                        <dt className="text-sm text-muted-foreground">E-posta</dt>
                        <dd className="flex flex-wrap items-center gap-2">
                            <span className="break-all">{profile.email}</span>
                            {profile.emailVerified ? (
                                <Badge className="bg-success-soft text-success">Doğrulandı</Badge>
                            ) : (
                                <Badge className="bg-scarce-soft text-scarce">Doğrulanmadı</Badge>
                            )}
                        </dd>
                        {!profile.emailVerified && (
                            <dd className="text-sm text-muted-foreground sm:col-start-2">
                                Kayıt olurken gönderdiğimiz e-postadaki bağlantıya tıkla.
                            </dd>
                        )}
                    </dl>
                </section>

                <section className="mt-6 rounded-xl border bg-card p-6">
                    <h2 className="text-xl font-semibold">Etkinlik düzenle</h2>
                    {isOrganizer ? (
                        <p className="mt-3 leading-relaxed text-muted-foreground">
                            Organizatörsün. Etkinlik oluşturma ve yönetme ekranları çok yakında
                            burada olacak.
                        </p>
                    ) : (
                        <>
                            <p className="mt-3 mb-5 leading-relaxed text-muted-foreground">
                                Kendi etkinliğini kurmak için organizatör ol. Ücretsiz; mekanda ya
                                da kendi adresinde, ücretli ya da ücretsiz etkinlik
                                düzenleyebilirsin.
                            </p>
                            <BecomeOrganizerForm />
                        </>
                    )}
                </section>

                <div className="mt-10">
                    <LogoutButton />
                </div>
            </main>
        </>
    );
}
