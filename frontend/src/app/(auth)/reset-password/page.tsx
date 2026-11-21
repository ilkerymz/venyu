import type { Metadata } from "next";
import { AuthHeading } from "../auth-heading";
import { MissingTokenNotice } from "../link-problem";
import { ResetPasswordForm } from "./reset-form";

export const metadata: Metadata = { title: "Yeni şifre belirle", robots: { index: false } };

/** Backend'in şifre sıfırlama e-postasındaki bağlantı: /reset-password?token=… (ADR-004). */
export default async function ResetPasswordPage({
    searchParams,
}: {
    searchParams: Promise<{ token?: string }>;
}) {
    const { token } = await searchParams;
    return (
        <>
            <AuthHeading title="Yeni şifre belirle" />
            {token ? (
                <ResetPasswordForm token={token} />
            ) : (
                <MissingTokenNotice retryHref="/sifremi-unuttum" retryLabel="Yeni bağlantı iste" />
            )}
        </>
    );
}
