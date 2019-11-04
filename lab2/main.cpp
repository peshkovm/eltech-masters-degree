#include <iostream>
#include "omp.h"
#include <stdlib.h> // srand, rand
#include <time.h>   // time
#include <math.h>

using namespace std;

void swap(int &from, int &to) {
    int tmp = from;
    from = to;
    to = tmp;
}

void trickleDown(int arr[], int lastPos, int parent) {
    int left = 2 * parent + 1;
    int right = 2 * parent + 2;
    int largest = parent;

    if (left <= lastPos && arr[left] > arr[largest]) {
        largest = left;
    }

    if (right <= lastPos && arr[right] > arr[largest]) {
        largest = right;
    }

    if (largest != parent) {
        swap(arr[parent], arr[largest]);
        trickleDown(arr, lastPos, largest);
    }
}

void trickleUp(int arr[], int lastPos, int position) {
    if (position == 0)
        return;

    int parent = (int) floor((position - 1) / 2);

    if (arr[position] > arr[parent]) {
        swap(arr[position], arr[parent]);
        trickleUp(arr, lastPos, parent);
    }
}

void remove(int arr[], int &lastPos) {
    swap(arr[0], arr[lastPos--]);
    trickleDown(arr, lastPos, 0);
}

void add(int arr[], int &lastPos, int newElement) {
    arr[++lastPos] = newElement;
    trickleUp(arr, lastPos, lastPos);
}

/* A utility function to print array of size n */
void printArray(int arr[], int n) {
    for (int i = 0; i < n; ++i)
        cout << arr[i] << " ";
    cout << "\n";
}

void heapSort(int arr[], int lastPos, int arrLength) {
    for (int i = 0; i < arrLength - 1; i++) {
        remove(arr, lastPos);
    }
}

int main() {
    int arrLength = 0;
    int lastPos = -1;

    cout << "Enter count of elements in array: ";
    cin >> arrLength;

    int *arr = new int[arrLength];

    cout << endl;

    for (int i = 0; i < arrLength; i++) {
        arr[i] = 0; //clear array for beautiful output
    }

    srand(time(nullptr));

    double start = omp_get_wtime();

    for (int i = 0; i < arrLength; i++) {
        int randomNumber = 1 + (rand() % (arrLength - 1 + 1));
        add(arr, lastPos, randomNumber);
    }

    double stop = omp_get_wtime();

    double execTime = stop - start;

    cout << "In array: " << endl;
    printArray(arr, arrLength);

    start = omp_get_wtime();

    heapSort(arr, lastPos, arrLength);

    stop = omp_get_wtime();

    cout << endl;
    cout << "Execution time  = " << execTime + (stop - start) << endl;
    cout << "Result:" << endl;
    printArray(arr, arrLength);
}
