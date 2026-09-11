export interface StandardError {
  timeStamp: number;
  status: number;
  error: string;
  message: string;
  path: string;
  code: string | null;
  details: Record<string, unknown>;
}
