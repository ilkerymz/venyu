import type { Metadata } from "next";
import { Suspense } from "react";
import { FormSkeleton } from "../form-skeleton";
import { AuthHeading } from "../auth-heading";
import { MissingTokenNotice } from "../link-problem";
import { ResetPasswordForm } from "./reset-form";

export const metadata: Metadata = { title: "Yeni şifre belirle", robots: { index: false } };

/** Backend'in şifre sıfırlama e-postasındaki bağlantı: /reset-password?token=… (ADR-004). */
type TokenProps = { searchParams: Promise<{ token?: string }> };

export default function ResetPasswordPage({ searchParams }: TokenProps) {
    return (
        <>
            <AuthHeading title="Yeni şifre belirle" />
            <Suspense fallback={<FormSkeleton />}>
                <ResetBody searchParams={searchParams} />
            </Suspense>
        </>
    );
}

async function ResetBody({ searchParams }: TokenProps) {
    const { token } = await searchParams;
    return token ? (
        <ResetPasswordForm token={token} />
    ) : (
        <MissingTokenNotice retryHref="/sifremi-unuttum" retryLabel="Yeni bağlantı iste" />
    );
}
