#ifndef SAMPLEPORT_H
#define SAMPLEPORT_H

/* Forward declaration only: Kotlin sees it as cnames.structs.Opaque. */
struct Opaque;

typedef struct Point {
    int x;
    int y;
} Point;

static inline struct Opaque* sampleport_open(void) { return 0; }
static inline Point sampleport_origin(void) { Point p = {0, 0}; return p; }

#endif
