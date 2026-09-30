#ifndef SPIKE_FIXTURE_H
#define SPIKE_FIXTURE_H
struct Opaque;
typedef struct Point { int x; int y; } Point;
struct Opaque* opaque_new(void);
int point_sum(Point p);
#endif
