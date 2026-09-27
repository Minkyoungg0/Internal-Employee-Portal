import { useEffect, useState, type DependencyList } from 'react';

interface UsePollingOptions<T> {
  intervalMs: number;
  initialData: T;
  shouldContinue?: (result: T) => boolean;
  errorMessage?: string;
}

/**
 * 서버 상태를 주기적으로 다시 조회하는 공통 패턴(요청 → 성공 시 다음 조회 예약 →
 * 언마운트/deps 변경 시 중단)을 추출한 훅. 목록 자동 갱신처럼 항상 반복하는 경우와,
 * 검사 결과처럼 조건(`shouldContinue`)이 참인 동안만 반복하는 경우를 둘 다 지원한다.
 */
export function usePolling<T>(
  fetcher: (signal: AbortSignal) => Promise<T>,
  { intervalMs, initialData, shouldContinue = () => true, errorMessage = '요청을 처리하지 못했습니다.' }: UsePollingOptions<T>,
  deps: DependencyList,
) {
  const [data, setData] = useState<T>(initialData);
  const [error, setError] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout> | undefined;

    async function tick() {
      try {
        const result = await fetcher(controller.signal);
        if (controller.signal.aborted) return;
        setData(result);
        setError('');
        if (shouldContinue(result)) timer = setTimeout(() => void tick(), intervalMs);
      } catch (e) {
        if (!controller.signal.aborted) setError(e instanceof Error ? e.message : errorMessage);
      }
    }

    void tick();
    return () => {
      controller.abort();
      if (timer) clearTimeout(timer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { data, setData, error, setError } as const;
}
