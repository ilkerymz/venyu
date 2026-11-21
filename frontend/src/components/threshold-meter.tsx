import { cn } from "@/lib/utils";

/** Bu sayının üstünde kişi başı kutu okunmaz; sürekli çubuğa geçilir. */
const MAX_SEGMENTS = 30;

type ThresholdMeterProps = {
    /** Etkinliğin gerçekleşmesi için gereken en az katılımcı. */
    minimum: number;
    /** Şu ana kadar katılan kişi sayısı. */
    current: number;
    className?: string;
};

/**
 * Minimum katılımcı garantisinin göstergesi. Her kutu bir kişidir:
 * eşik dolana kadar çini mavisi, dolunca turkuaz.
 */
export function ThresholdMeter({ minimum, current, className }: ThresholdMeterProps) {
    const reached = current >= minimum;
    const missing = Math.max(minimum - current, 0);
    const label = reached
        ? `${minimum} kişilik eşik doldu, etkinlik kesinleşti.`
        : `${missing} kişi daha katılırsa etkinlik kesinleşir.`;

    return (
        <div className={cn("space-y-2", className)}>
            <div
                role="meter"
                aria-label="Minimum katılımcı eşiği"
                aria-valuemin={0}
                aria-valuemax={minimum}
                aria-valuenow={Math.min(current, minimum)}
                aria-valuetext={label}
                className="flex gap-[3px]"
            >
                {minimum <= MAX_SEGMENTS ? (
                    Array.from({ length: minimum }, (_, i) => (
                        <span
                            key={i}
                            className={cn(
                                "h-2.5 flex-1 rounded-[2px]",
                                i < current ? (reached ? "bg-success" : "bg-primary") : "bg-muted",
                            )}
                        />
                    ))
                ) : (
                    <span className="h-2.5 flex-1 overflow-hidden rounded-[2px] bg-muted">
                        <span
                            className={cn("block h-full", reached ? "bg-success" : "bg-primary")}
                            style={{ width: `${Math.min(current / minimum, 1) * 100}%` }}
                        />
                    </span>
                )}
            </div>
            <p className="text-sm text-muted-foreground nums">
                <span className={cn("font-semibold", reached ? "text-success" : "text-foreground")}>
                    {current}/{minimum}
                </span>{" "}
                {label}
            </p>
        </div>
    );
}
