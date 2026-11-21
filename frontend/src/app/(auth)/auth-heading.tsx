export function AuthHeading({ title, children }: { title: string; children?: React.ReactNode }) {
    return (
        <div className="mb-8">
            <h1 className="text-4xl font-bold tracking-[-0.02em] [font-stretch:85%]">{title}</h1>
            {children && (
                <p className="mt-3 text-base leading-relaxed text-muted-foreground">{children}</p>
            )}
        </div>
    );
}
